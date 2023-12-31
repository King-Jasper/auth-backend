package com.mintfintech.savingsms.usecase.features.loan.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.LienAccountRequestCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.LienAccountResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.LoanDetailResponseCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.LoanDeclineEmailEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.LoanEmailEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.LoanRepaymentEmailEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.LoanUpdateEvent;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.loan.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.features.loan.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.features.loan.LoanRepaymentUseCase;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanRepaymentUseCaseImpl implements LoanRepaymentUseCase {

    private static final int NUMBER_OF_DAYS_UPFRONT = 2;
    private final ApplicationProperty applicationProperty;
    private final LoanRequestEntityDao loanRequestEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final CustomerLoanProfileUseCase customerLoanProfileUseCase;
    private final LoanTransactionEntityDao loanTransactionEntityDao;
    private final GetLoansUseCase getLoansUseCase;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final CoreBankingServiceClient coreBankingServiceClient;
    private final UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase;
    private final ApplicationEventService applicationEventService;
    private final SystemIssueLogService systemIssueLogService;
    private final MintAccountEntityDao mintAccountEntityDao;


    @Override
    public void dispatchEmailToCustomersWithPaymentDueInTwoDays() {
        List<LoanRequestEntity> paymentsDueInTwoDays = loanRequestEntityDao.getLoanRepaymentDueInDays(NUMBER_OF_DAYS_UPFRONT);

        for (LoanRequestEntity loan : paymentsDueInTwoDays) {

            AppUserEntity appUser = appUserEntityDao.getRecordById(loan.getRequestedBy().getId());

            LoanRepaymentEmailEvent event = LoanRepaymentEmailEvent.builder()
                    .loanBalance(loan.getRepaymentAmount().subtract(loan.getAmountPaid()))
                    .amountPaid(loan.getAmountPaid())
                    .customerName(appUser.getName())
                    .loanDueDate(loan.getRepaymentDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .recipient(appUser.getEmail())
                    .build();

            applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REPAYMENT_REMINDER, new EventModel<>(event));
            event.setRecipient("loanadmin@mintyn.com");
            applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REPAYMENT_REMINDER, new EventModel<>(event));

        }
    }

    @Override
    public void loanRepaymentDueToday() {
        List<LoanRequestEntity> repaymentDueToday = loanRequestEntityDao.getLoanRepaymentDueToday();

        if (repaymentDueToday.isEmpty()) {
            return;
        }

       // Calendar calendar = Calendar.getInstance();
       // boolean isWeekend = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;

        repaymentDueToday.forEach(loan -> {
            /*if(isWeekend) {
                loan.setRepaymentDueDate(loan.getRepaymentDueDate().plusDays(1));
                loanRequestEntityDao.saveRecord(loan);
                return; // next the current iteration.
            }
            */
            List<LoanTransactionEntity> transactions = loanTransactionEntityDao.getLoanTransactions(loan);

            for (LoanTransactionEntity transaction : transactions) {
                if (transaction.isLienActive()) {
                    removeLienFromAccount(loan, transaction);
                }
            }
            sendLoanRepaymentEmail(loan);
        });
    }

    @Override
    @Transactional
    public LoanModel repayment(AuthenticatedUser currentUser, double amount, String loanId) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

        LoanRequestEntity loan = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        if (loan.getApprovalStatus() != ApprovalStatusConstant.DISBURSED) {
            throw new BadRequestException("This loan was not disbursed");
        }

        if (loan.getRepaymentStatus() == LoanRepaymentStatusConstant.PAID) {
            throw new BadRequestException("This loan has been repaid in full already");
        }

        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getRecordById(loan.getBankAccount().getId());
        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);

        BigDecimal amountToPay = BigDecimal.valueOf(amount);
        BigDecimal amountPending = loan.getRepaymentAmount().subtract(loan.getAmountPaid());

        if (debitAccount.getAvailableBalance().compareTo(amountToPay) < 0) {
            throw new BusinessLogicConflictException("Insufficient balance to make this loan repayment. " +
                    "Loan Repayment Amount: " + amount + ". Available Balance: " + debitAccount.getAvailableBalance());
        }

        if (amountToPay.compareTo(amountPending) > 0) {
            amountToPay = amountPending;
        }

        Optional<LoanTransactionEntity> optionalLoanTransaction = placeLienOnAccount(loan, amountToPay, debitAccount.getAccountNumber());

        if (optionalLoanTransaction.isPresent()) {
            BigDecimal amountPaid = loan.getAmountPaid().add(amountToPay);
            LoanRepaymentStatusConstant repaymentStatusConstant = amountPaid.compareTo(loan.getRepaymentAmount()) < 0 ? LoanRepaymentStatusConstant.PARTIALLY_PAID : LoanRepaymentStatusConstant.PAID;
            loan.setAmountPaid(amountPaid);
            loan.setRepaymentStatus(repaymentStatusConstant);
            if(repaymentStatusConstant == LoanRepaymentStatusConstant.PAID){
                loan.setActiveLoan(false);
            }
            loan = loanRequestEntityDao.saveRecord(loan);
            LoanEmailEvent event = LoanEmailEvent.builder()
                    .customerName(appUser.getName())
                    .recipient(appUser.getEmail())
                    .build();

            applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REPAYMENT_SUCCESS, new EventModel<>(event));

        } else {
            LoanDeclineEmailEvent event = LoanDeclineEmailEvent.builder()
                    .reason(StringUtils.defaultString("Transaction Declined"))
                    .customerName(appUser.getName())
                    .recipient(appUser.getEmail())
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REPAYMENT_FAILURE, new EventModel<>(event));

            throw new BusinessLogicConflictException("Loan Repayment Transaction Failed. Please try again later!!");
        }

        if (loan.getRepaymentStatus() == LoanRepaymentStatusConstant.PAID) {
            customerLoanProfileUseCase.updateCustomerRating(appUser);
        } else {
            LoanRepaymentEmailEvent event = LoanRepaymentEmailEvent.builder()
                    .loanBalance(loan.getRepaymentAmount().subtract(loan.getAmountPaid()))
                    .amountPaid(loan.getAmountPaid())
                    .customerName(appUser.getName())
                    .loanDueDate(loan.getRepaymentDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .recipient(appUser.getEmail())
                    .build();

            applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_PARTIAL_REPAYMENT_SUCCESS, new EventModel<>(event));
        }

        return getLoansUseCase.toLoanModel(loan);
    }

    @Override
    public void checkDueLoanPendingDebit() {

        List<LoanRequestEntity> loans = loanRequestEntityDao.getPendingDebitLoans();
        //log.info("loan size - {}", loans.size());

        if (loans.isEmpty()) {
            return;
        }

        for (LoanRequestEntity loan : loans) {

            MintBankAccountEntity bankAccount = mintBankAccountEntityDao.getRecordById(loan.getBankAccount().getId());

            MintAccountEntity mintAccount = mintAccountEntityDao.getRecordById(bankAccount.getMintAccount().getId());

            MsClientResponse<LoanDetailResponseCBS> msClientResponse = coreBankingServiceClient.getLoanDetails(mintAccount.getBankOneCustomerId(), loan.getBankOneAccountNumber());

           // log.info("loan id - {} - {}", loan.getLoanId(), msClientResponse.getData().toString());

            boolean completed = false;
            if (msClientResponse.getStatusCode() == HttpStatus.OK.value() && msClientResponse.isSuccess()) {
                LoanDetailResponseCBS responseCBS = msClientResponse.getData();
                System.out.println("paid off - "+ (responseCBS.getTotalOutstandingAmount().compareTo(BigDecimal.ZERO) == 0));
                if(responseCBS.getTotalOutstandingAmount().compareTo(BigDecimal.ZERO) == 0) {
                    loan.setAmountPaid(responseCBS.getTotalAmountPaid());
                    loan.setRepaymentStatus(LoanRepaymentStatusConstant.COMPLETED);
                    loan.setActiveLoan(false);
                    completed = true;
                }
                loan.setAmountCollectedOnBankOne(responseCBS.getTotalAmountPaid());
                loanRequestEntityDao.saveRecord(loan);
                if(completed) {
                    LoanUpdateEvent updateEvent = LoanUpdateEvent.builder()
                            .loanId(loan.getLoanId())
                            .loanType(loan.getLoanType().name())
                            .updateType(LoanUpdateEvent.UpdateType.REPAID.name())
                            .build();
                    applicationEventService.publishEvent(ApplicationEventService.EventType.LOAN_UPDATE, new EventModel<>(updateEvent));
                }
            }

        }
    }

    @Override
    public void loanRepaymentOverDue() {
        List<LoanRequestEntity> repaymentOverDue = loanRequestEntityDao.getOverdueLoanRepayment();

        if (repaymentOverDue.isEmpty()) {
            return;
        }
        repaymentOverDue.forEach(this::sendLoanRepaymentEmail);
    }

    private void sendLoanRepaymentEmail(LoanRequestEntity loan) {
        AppUserEntity appUser = appUserEntityDao.getRecordById(loan.getRequestedBy().getId());
        MintBankAccountEntity bankAccount = mintBankAccountEntityDao.getRecordById(loan.getBankAccount().getId());
        LoanEmailEvent loanEmailEvent = LoanEmailEvent
                .builder()
                .customerName(appUser.getName())
                .recipient(appUser.getEmail())
                .accountNumber(bankAccount.getAccountNumber())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REPAYMENT_DUE, new EventModel<>(loanEmailEvent));
        loanEmailEvent.setRecipient(applicationProperty.getLoanAdminEmail());
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REPAYMENT_DUE, new EventModel<>(loanEmailEvent));
    }

    private void removeLienFromAccount(LoanRequestEntity loan, LoanTransactionEntity transaction) {

        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getRecordById(loan.getBankAccount().getId());

        LienAccountRequestCBS request = LienAccountRequestCBS.builder()
                .accountNumber(debitAccount.getAccountNumber())
                .referenceId(transaction.getLienReference())
                .build();

        MsClientResponse<LienAccountResponseCBS> msClientResponse = coreBankingServiceClient.removeLienOnAccount(request);

        if (msClientResponse.getStatusCode() == HttpStatus.OK.value()
                && msClientResponse.isSuccess()
                && msClientResponse.getData().isSuccess()) {

            transaction.setLienActive(false);
            loanTransactionEntityDao.saveRecord(transaction);
        } else {
            String message = String.format("Lien Reference: %s; Account number: %s ; message: %s", transaction.getLienReference(), debitAccount.getAccountNumber(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Lien Removal Failure", "Account Lien Removal Failed", message);
        }
    }

    private Optional<LoanTransactionEntity> placeLienOnAccount(LoanRequestEntity loan, BigDecimal amountToPay, String debitAccountNumber) {

        LienAccountRequestCBS request = LienAccountRequestCBS.builder()
                .accountNumber(debitAccountNumber)
                .amount(amountToPay)
                .reason("Loan Repayment for " + loan.getLoanId())
                .build();

        MsClientResponse<LienAccountResponseCBS> msClientResponse = coreBankingServiceClient.placeLienOnAccount(request);

        if (msClientResponse.getStatusCode() == HttpStatus.OK.value()
                && msClientResponse.isSuccess()
                && msClientResponse.getData().isSuccess()) {

            LienAccountResponseCBS responseCBS = msClientResponse.getData();

            LoanTransactionEntity transaction = LoanTransactionEntity.builder()
                    .loanRequest(loan)
                    .transactionAmount(amountToPay)
                    .status(TransactionStatusConstant.SUCCESSFUL)
                    .transactionType(TransactionTypeConstant.DEBIT)
                    .lienActive(true)
                    .transactionReference(responseCBS.getReferenceID())
                    .lienReference(responseCBS.getReferenceID())
                    .responseCode(responseCBS.getStatus())
                    .responseMessage(responseCBS.getMessage())
                    .build();
            return Optional.of(loanTransactionEntityDao.saveRecord(transaction));
        }
        return Optional.empty();
    }
}
