package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanTransactionEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
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
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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

            //call core banking
        });


    }

    @Override
    public void processPaymentOfDueRepayment() {

        List<LoanRequestEntity> defaultedRepayments = loanRequestEntityDao.getDefaultedUnpaidLoanRepayment();

        for (LoanRequestEntity loanRequestEntity : defaultedRepayments) {
            //call core banking
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
            throw new BusinessLogicConflictException("Insufficient balance to make this loan repayment: " + loan.getLoanId());
        }

        if (amountToPay.compareTo(amountPending) > 0) {
            amountToPay = amountPending;
        }

        LoanTransactionEntity transaction = debitCustomerAccount(loan, amountToPay, debitAccount.getAccountNumber());

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
    public void processRecoverySuspenseAccountToLoanAccount() {
        List<LoanTransactionEntity> loanTransactions = loanTransactionEntityDao.getLoansPendingRepayment(LoanTransactionTypeConstant.RECOVERY_TO_MINT);

        for (LoanTransactionEntity transaction : loanTransactions) {
            LoanRequestEntity loan = loanRequestEntityDao.getRecordById(transaction.getLoanRequest().getId());
            moveFundFromLoanRecoverySuspenseAccountToMintLoanAccount(loan, transaction);
        }
    }

    @Override
    public void processLoanRecoverySuspenseAccountToInterestReceivableAccount() {
        List<LoanTransactionEntity> loanTransactions = loanTransactionEntityDao.getLoansPendingRepayment(LoanTransactionTypeConstant.RECOVERY_TO_INTEREST);

        for (LoanTransactionEntity transaction : loanTransactions) {
            LoanRequestEntity loan = loanRequestEntityDao.getRecordById(transaction.getLoanRequest().getId());
            moveFundFromLoanRecoverySuspenseAccountToInterestReceivableAccount(loan, transaction);
        }
    }

    @Override
    public void processInterestIncomeSuspenseAccountToInterestIncomeAccount() {
        List<LoanTransactionEntity> loanTransactions = loanTransactionEntityDao.getLoansPendingRepayment(LoanTransactionTypeConstant.SUSPENSE_TO_INCOME);

        for (LoanTransactionEntity transaction : loanTransactions) {
            LoanRequestEntity loan = loanRequestEntityDao.getRecordById(transaction.getLoanRequest().getId());
            moveFundFromInterestIncomeSuspenseAccountToInterestIncomeAccount(loan, transaction);
        }
    }

    private LoanTransactionEntity debitCustomerAccount(LoanRequestEntity loan, BigDecimal amountToPay, String accountNumber) {

        LoanTransactionEntity transaction = LoanTransactionEntity.builder()
                .loanRequest(loan)
                .autoDebit(false)
                .transactionAmount(amountToPay)
                .transactionReference(loanRequestEntityDao.generateLoanTransactionRef())
                .status(TransactionStatusConstant.PENDING)
                .transactionType(TransactionTypeConstant.DEBIT)
                .loanTransactionType(LoanTransactionTypeConstant.CUSTOMER_TO_RECOVERY)
                .build();

        transaction = loanTransactionEntityDao.saveRecord(transaction);

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(transaction.getTransactionAmount())
                .loanTransactionType(LoanTransactionTypeConstant.CUSTOMER_TO_RECOVERY.name())
                .narration(constructLoanRepaymentNarration(loan))
                .reference(transaction.getTransactionReference())
                .accountNumber(accountNumber)
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanRepayment(request);

        if (msClientResponse.getData() != null) {
            transaction.setResponseCode(msClientResponse.getData().getResponseCode());
            transaction.setResponseMessage(msClientResponse.getData().getResponseMessage());
            transaction.setExternalReference(msClientResponse.getData().getBankOneReference());
            transaction = loanTransactionEntityDao.saveRecord(transaction);
        }

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), transaction.getTransactionReference(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Customer To Loan Recovery Suspense funding failed", message);
            transaction.setStatus(TransactionStatusConstant.FAILED);
        } else {
            transaction.setStatus(TransactionStatusConstant.SUCCESSFUL);
        }
        return loanTransactionEntityDao.saveRecord(transaction);
    }

    private void moveFundsForFullyPaidLoans(LoanRequestEntity loan) {

        LoanTransactionEntity creditLoanAccount = LoanTransactionEntity.builder()
                .loanRequest(loan)
                .autoDebit(false)
                .transactionAmount(loan.getLoanAmount())
                .transactionReference(loanRequestEntityDao.generateLoanTransactionRef())
                .status(TransactionStatusConstant.PENDING)
                .transactionType(TransactionTypeConstant.DEBIT)
                .loanTransactionType(LoanTransactionTypeConstant.RECOVERY_TO_MINT)
                .build();

        loanTransactionEntityDao.saveRecord(creditLoanAccount);

        LoanTransactionEntity creditInterestRecAccount = LoanTransactionEntity.builder()
                .loanRequest(loan)
                .autoDebit(false)
                .transactionAmount(loan.getRepaymentAmount().subtract(loan.getLoanAmount()))
                .transactionReference(loanRequestEntityDao.generateLoanTransactionRef())
                .status(TransactionStatusConstant.PENDING)
                .transactionType(TransactionTypeConstant.DEBIT)
                .loanTransactionType(LoanTransactionTypeConstant.RECOVERY_TO_INTEREST)
                .build();

        loanTransactionEntityDao.saveRecord(creditInterestRecAccount);

        LoanTransactionEntity creditIncomeAccount = LoanTransactionEntity.builder()
                .loanRequest(loan)
                .autoDebit(false)
                .transactionAmount(loan.getRepaymentAmount().subtract(loan.getLoanAmount()))
                .transactionReference(loanRequestEntityDao.generateLoanTransactionRef())
                .status(TransactionStatusConstant.PENDING)
                .transactionType(TransactionTypeConstant.DEBIT)
                .loanTransactionType(LoanTransactionTypeConstant.SUSPENSE_TO_INCOME)
                .build();

        loanTransactionEntityDao.saveRecord(creditIncomeAccount);
    }

    private void moveFundFromLoanRecoverySuspenseAccountToMintLoanAccount(LoanRequestEntity loan, LoanTransactionEntity transaction) {

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(transaction.getTransactionAmount())
                .loanTransactionType(LoanTransactionTypeConstant.RECOVERY_TO_MINT.name())
                .narration(constructLoanRepaymentNarration(loan))
                .reference(transaction.getTransactionReference())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanRepayment(request);

        if (msClientResponse.getData() != null) {
            transaction.setResponseCode(msClientResponse.getData().getResponseCode());
            transaction.setResponseMessage(msClientResponse.getData().getResponseMessage());
            transaction.setExternalReference(msClientResponse.getData().getBankOneReference());
            transaction = loanTransactionEntityDao.saveRecord(transaction);
        }

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), transaction.getTransactionReference(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Loan Recovery Suspense To Mint funding failed", message);
        } else {
            transaction.setStatus(TransactionStatusConstant.SUCCESSFUL);
            loanTransactionEntityDao.saveRecord(transaction);
        }
    }

    private void moveFundFromLoanRecoverySuspenseAccountToInterestReceivableAccount(LoanRequestEntity loan, LoanTransactionEntity transaction) {

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(transaction.getTransactionAmount())
                .loanTransactionType(LoanTransactionTypeConstant.RECOVERY_TO_INTEREST.name())
                .narration(constructLoanRepaymentNarration(loan))
                .reference(transaction.getTransactionReference())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanRepayment(request);

        if (msClientResponse.getData() != null) {
            transaction.setResponseCode(msClientResponse.getData().getResponseCode());
            transaction.setResponseMessage(msClientResponse.getData().getResponseMessage());
            transaction.setExternalReference(msClientResponse.getData().getBankOneReference());
            transaction = loanTransactionEntityDao.saveRecord(transaction);
        }

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), transaction.getTransactionReference(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Loan Recovery Suspense To Interest funding failed", message);
        } else {
            transaction.setStatus(TransactionStatusConstant.SUCCESSFUL);
            loanTransactionEntityDao.saveRecord(transaction);
        }
    }

    private void moveFundFromInterestIncomeSuspenseAccountToInterestIncomeAccount(LoanRequestEntity loan, LoanTransactionEntity transaction) {

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(transaction.getTransactionAmount())
                .loanTransactionType(LoanTransactionTypeConstant.SUSPENSE_TO_INCOME.name())
                .narration(constructLoanRepaymentNarration(loan))
                .reference(transaction.getTransactionReference())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanRepayment(request);

        if (msClientResponse.getData() != null) {
            transaction.setResponseCode(msClientResponse.getData().getResponseCode());
            transaction.setResponseMessage(msClientResponse.getData().getResponseMessage());
            transaction.setExternalReference(msClientResponse.getData().getBankOneReference());
            transaction = loanTransactionEntityDao.saveRecord(transaction);
        }

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), transaction.getTransactionReference(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Interest Income Suspense To Interest Income funding failed", message);
        } else {
            transaction.setStatus(TransactionStatusConstant.SUCCESSFUL);
            loanTransactionEntityDao.saveRecord(transaction);
        }
    }

    private String constructLoanRepaymentNarration(LoanRequestEntity loanRequestEntity) {
        String narration = String.format("LR-%s %s", loanRequestEntity.getLoanId(), loanRequestEntity.getLoanType().name());
        if (narration.length() > 61) {
            return narration.substring(0, 60);
        }
        return narration;
    }


}
