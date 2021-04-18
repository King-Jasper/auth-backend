package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRepaymentEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanTransactionEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.LoanRepaymentEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.LoanTransactionEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.LoanTransactionRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.LoanRepaymentUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.AccountCreditEvent;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoanRepaymentUseCaseImpl implements LoanRepaymentUseCase {

    private static final int NUMBER_OF_DAYS_UPFRONT = 2;
    private final LoanRequestEntityDao loanRequestEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final CustomerLoanProfileUseCase customerLoanProfileUseCase;
    private final LoanTransactionEntityDao loanTransactionEntityDao;
    private final GetLoansUseCase getLoansUseCase;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final CoreBankingServiceClient coreBankingServiceClient;
    private final SystemIssueLogService systemIssueLogService;
    private final UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase;
    private final LoanRepaymentEntityDao repaymentEntityDao;


    @Override
    public void dispatchEmailToCustomersWithPaymentDueInTwoDays() {
        List<LoanRequestEntity> paymentsDueInTwoDays = loanRequestEntityDao.getLoanRepaymentDueInDays(NUMBER_OF_DAYS_UPFRONT);
    }

    @Override
    public void dispatchEmailNotificationRepaymentOnDueDay() {
        List<LoanRequestEntity> repaymentDueToday = loanRequestEntityDao.getLoanRepaymentDueToday();
    }

    @Override
    public void checkDefaultedRepayment() {
        List<LoanRequestEntity> repaymentDueToday = loanRequestEntityDao.getLoanRepaymentDueToday();

        repaymentDueToday.forEach(loanRequestEntity -> {
            loanRequestEntity.setRepaymentStatus(LoanRepaymentStatusConstant.FAILED);
            loanRequestEntityDao.saveRecord(loanRequestEntity);

            AppUserEntity loanOwner = appUserEntityDao.getRecordById(loanRequestEntity.getRequestedBy().getId());

            customerLoanProfileUseCase.updateCustomerRating(loanOwner);
        });
    }

    @Override
    public void processPaymentOfOverDueRepayment(AccountCreditEvent accountCredit) {
        Optional<MintBankAccountEntity> bankAccountEntityOptional = mintBankAccountEntityDao.findByAccountNumber(accountCredit.getAccountNumber());
        if (!bankAccountEntityOptional.isPresent()) {
            return;
        }

        List<LoanRequestEntity> overDueLoanRepayments = loanRequestEntityDao.getOverdueLoanRepayment(bankAccountEntityOptional.get());

        if (overDueLoanRepayments.isEmpty()){
            return;
        }

        for (LoanRequestEntity loan : overDueLoanRepayments) {

            BigDecimal amountToPay = loan.getRepaymentAmount().subtract(loan.getAmountPaid());

            MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getRecordById(loan.getBankAccount().getId());
            debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);

            BigDecimal availableBalance = debitAccount.getAvailableBalance();

            if (availableBalance.compareTo(amountToPay) < 0){
                amountToPay = availableBalance;
            }

            LoanTransactionEntity transaction = debitCustomerAccount(loan, true, amountToPay, debitAccount.getAccountNumber());

            if (transaction.getStatus() == TransactionStatusConstant.SUCCESSFUL){
                loan.setAmountPaid(loan.getAmountPaid().add(amountToPay));
                loan = loanRequestEntityDao.saveRecord(loan);

                if (loan.getRepaymentAmount().compareTo(loan.getAmountPaid()) == 0){
                    moveFundsForFullyPaidLoans(loan);
                }

            }

        }

    }

    @Override
    @Transactional
    public LoanModel repayment(AuthenticatedUser currentUser, double amount, String loanId) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

        LoanRequestEntity loan = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getRecordById(loan.getBankAccount().getId());
        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);

        BigDecimal amountToPay = BigDecimal.valueOf(amount);
        BigDecimal amountPending = loan.getRepaymentAmount().subtract(loan.getAmountPaid());

        if (debitAccount.getAvailableBalance().compareTo(amountToPay) < 0) {
            throw new BusinessLogicConflictException("Insufficient balance to make this loan repayment: " + debitAccount.getAvailableBalance());
        }

        if (amountToPay.compareTo(amountPending) > 0) {
            amountToPay = amountPending;
        }

        LoanTransactionEntity transaction = debitCustomerAccount(loan, false, amountToPay, debitAccount.getAccountNumber());

        if (transaction.getStatus() == TransactionStatusConstant.SUCCESSFUL) {
            BigDecimal amountPaid = loan.getAmountPaid().add(amountToPay);
            loan.setAmountPaid(amountPaid);
            loan.setRepaymentStatus(amountPaid.compareTo(loan.getRepaymentAmount()) < 0 ? LoanRepaymentStatusConstant.PARTIALLY_PAID : LoanRepaymentStatusConstant.PAID);
            loan = loanRequestEntityDao.saveRecord(loan);
        } else {
            throw new BusinessLogicConflictException("Loan Repayment Transaction Failed. Please try again later!!");
        }

        if (loan.getRepaymentStatus() == LoanRepaymentStatusConstant.PAID) {
            customerLoanProfileUseCase.updateCustomerRating(appUser);
            moveFundsForFullyPaidLoans(loan);
        }

        return getLoansUseCase.toLoanModel(loan);
    }

    @Override
    public void processLoanRecoverySuspenseAccountToMintLoanAccount() {
        List<LoanRepaymentEntity> repayments = repaymentEntityDao.getPendingRecoveryToMintTransaction();

        for (LoanRepaymentEntity repayment : repayments) {
            LoanRequestEntity loan = loanRequestEntityDao.getRecordById(repayment.getLoanRequest().getId());
            creditMintLoanAccount(loan, repayment);
        }
    }

    @Override
    public void processInterestIncomeSuspenseAccountToInterestIncomeAccount() {
        List<LoanRepaymentEntity> repayments = repaymentEntityDao.getPendingSuspenseToIncomeTransaction();

        for (LoanRepaymentEntity repayment : repayments) {
            LoanRequestEntity loan = loanRequestEntityDao.getRecordById(repayment.getLoanRequest().getId());
            creditLoanInterestIncomeAccount(loan, repayment);
        }
    }

    private LoanTransactionEntity debitCustomerAccount(LoanRequestEntity loan, Boolean isAutoDebit, BigDecimal amountToPay, String debitAccountNumber){

        String ref = loanRequestEntityDao.generateLoanTransactionRef();

        LoanTransactionEntity transaction = LoanTransactionEntity.builder()
                .loanRequest(loan)
                .autoDebit(isAutoDebit)
                .transactionAmount(amountToPay)
                .transactionReference(ref)
                .status(TransactionStatusConstant.PENDING)
                .transactionType(TransactionTypeConstant.DEBIT)
                .build();

        transaction = loanTransactionEntityDao.saveRecord(transaction);

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(transaction.getTransactionAmount())
                .loanTransactionType(LoanTransactionTypeConstant.CUSTOMER_TO_RECOVERY.name())
                .narration(constructLoanRepaymentNarration(loan, ref))
                .reference(ref)
                .accountNumber(debitAccountNumber)
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanRepayment(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), transaction.getTransactionReference(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Loan Repayment Issue", "Customer To Loan Recovery Suspense funding failed", message);
            transaction.setStatus(TransactionStatusConstant.FAILED);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            transaction.setResponseCode(responseCBS.getResponseCode());
            transaction.setResponseMessage(responseCBS.getResponseMessage());
            transaction.setExternalReference(responseCBS.getBankOneReference());

            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                transaction.setStatus(TransactionStatusConstant.SUCCESSFUL);
            } else {
                transaction.setStatus(TransactionStatusConstant.FAILED);
                String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), transaction.getTransactionReference(), msClientResponse.getMessage());
                systemIssueLogService.logIssue("Loan Repayment Issue", "Customer To Loan Recovery Suspense funding failed", message);
            }

        }
       return loanTransactionEntityDao.saveRecord(transaction);
    }

    private void moveFundsForFullyPaidLoans(LoanRequestEntity loan) {

        LoanRepaymentEntity loanRepayment = LoanRepaymentEntity.builder()
                .loanRequest(loan)
                .loanTransactionType(LoanTransactionTypeConstant.PENDING_RECOVERY_TO_MINT)
                .build();

        repaymentEntityDao.saveRecord(loanRepayment);
    }

    private void creditMintLoanAccount(LoanRequestEntity loan, LoanRepaymentEntity repayment) {

        String ref = loanRequestEntityDao.generateLoanId();

        repayment.setLoanRecoveryCreditReference(ref);
        repayment.setLoanTransactionType(LoanTransactionTypeConstant.PROCESSING_RECOVERY_TO_MINT);
        repaymentEntityDao.saveAndFlush(repayment);

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(loan.getLoanAmount())
                .loanTransactionType(LoanTransactionTypeConstant.RECOVERY_TO_MINT.name())
                .narration(constructLoanRepaymentNarration(loan, ref))
                .fee(loan.getLoanInterest())
                .reference(ref)
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanRepayment(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), ref, msClientResponse.getMessage());
            systemIssueLogService.logIssue("Loan Repayment Issue", "Loan Recovery Suspense To Mint funding failed", message);
            repayment.setLoanTransactionType(LoanTransactionTypeConstant.FAILED_RECOVERY_TO_MINT);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            repayment.setLoanRecoveryCreditCode(responseCBS.getResponseCode());

            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                repayment.setLoanTransactionType(LoanTransactionTypeConstant.PENDING_SUSPENSE_TO_INCOME);
            } else {
                repayment.setLoanTransactionType(LoanTransactionTypeConstant.FAILED_RECOVERY_TO_MINT);
                String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), ref, msClientResponse.getMessage());
                systemIssueLogService.logIssue("Loan Repayment Issue", "Loan Recovery Suspense To Mint funding failed", message);
            }
        }
        repaymentEntityDao.saveRecord(repayment);
    }

    private void creditLoanInterestIncomeAccount(LoanRequestEntity loan, LoanRepaymentEntity repayment) {

        String ref = loanRequestEntityDao.generateLoanId();

        repayment.setLoanIncomeCreditReference(ref);
        repayment.setLoanTransactionType(LoanTransactionTypeConstant.PROCESSING_SUSPENSE_TO_INCOME);
        repaymentEntityDao.saveAndFlush(repayment);

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(loan.getLoanInterest())
                .loanTransactionType(LoanTransactionTypeConstant.SUSPENSE_TO_INCOME.name())
                .narration(constructLoanRepaymentNarration(loan, ref))
                .reference(ref)
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanRepayment(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), ref, msClientResponse.getMessage());
            systemIssueLogService.logIssue("Loan Repayment Issue", "Interest Income Suspense To Interest Income funding failed", message);
            repayment.setLoanTransactionType(LoanTransactionTypeConstant.FAILED_SUSPENSE_TO_INCOME);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            repayment.setLoanIncomeCreditCode(responseCBS.getResponseCode());

            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                repayment.setLoanTransactionType(LoanTransactionTypeConstant.PROCESSED);
            } else {
                repayment.setLoanTransactionType(LoanTransactionTypeConstant.FAILED_SUSPENSE_TO_INCOME);
                String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), ref, msClientResponse.getMessage());
                systemIssueLogService.logIssue("Loan Repayment Issue", "Interest Income Suspense To Interest Income funding failed", message);
            }
        }
        repaymentEntityDao.saveRecord(repayment);
    }

    private String constructLoanRepaymentNarration(LoanRequestEntity loanRequestEntity, String reference) {
        String narration = String.format("LR-%s %s", loanRequestEntity.getLoanId(), reference);
        if (narration.length() > 61) {
            return narration.substring(0, 60);
        }
        return narration;
    }


}
