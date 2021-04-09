package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanTransactionEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.LoanTransactionEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.LoanTransactionRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.LoanApprovalUseCase;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanApprovalUseCaseImpl implements LoanApprovalUseCase {

    private final LoanRequestEntityDao loanRequestEntityDao;
    private final LoanTransactionEntityDao loanTransactionEntityDao;
    private final CoreBankingServiceClient coreBankingServiceClient;
    private final ApplicationProperty applicationProperty;
    private final GetLoansUseCase getLoansUseCase;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final SystemIssueLogService systemIssueLogService;

    @Transactional
    @Override
    public LoanModel approveLoanRequest(AuthenticatedUser authenticatedUser, String loanId, String reason, boolean approved) {

        LoanRequestEntity loanRequestEntity = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        if (approved) {

            loanRequestEntity.setApprovalStatus(ApprovalStatusConstant.APPROVED);
            loanRequestEntity.setApprovedDate(LocalDateTime.now());
            loanRequestEntity.setApproveByName(authenticatedUser.getUsername());
            loanRequestEntity.setApproveByUserId(authenticatedUser.getUserId());
            loanRequestEntity.setRepaymentDueDate(LocalDateTime.now().plusDays(applicationProperty.getPayDayLoanMaxTenor()));

            loanRequestEntityDao.saveRecord(loanRequestEntity);

            moveFundsForApprovedLoans(loanRequestEntity);

        } else {
            loanRequestEntity.setApprovalStatus(ApprovalStatusConstant.REJECTED);
            loanRequestEntity.setApproveByName(authenticatedUser.getUsername());
            loanRequestEntity.setApproveByUserId(authenticatedUser.getUserId());
            loanRequestEntity.setRejectionReason(reason);

            loanRequestEntityDao.saveRecord(loanRequestEntity);
        }

        return getLoansUseCase.toLoanModel(loanRequestEntity);
    }

    private void moveFundsForApprovedLoans(LoanRequestEntity loan) {

        LoanTransactionEntity creditLoanSuspenseAcct = LoanTransactionEntity.builder()
                .loanRequest(loan)
                .autoDebit(false)
                .transactionAmount(loan.getLoanAmount())
                .transactionReference(loanRequestEntityDao.generateLoanTransactionRef())
                .status(TransactionStatusConstant.PENDING)
                .transactionType(TransactionTypeConstant.CREDIT)
                .loanTransactionType(LoanTransactionTypeConstant.MINT_TO_SUSPENSE)
                .build();

        loanTransactionEntityDao.saveRecord(creditLoanSuspenseAcct);

        LoanTransactionEntity creditInterestIncomeSuspenseAcct = LoanTransactionEntity.builder()
                .loanRequest(loan)
                .autoDebit(false)
                .transactionAmount(loan.getRepaymentAmount().subtract(loan.getLoanAmount()))
                .transactionReference(loanRequestEntityDao.generateLoanTransactionRef())
                .status(TransactionStatusConstant.PENDING)
                .transactionType(TransactionTypeConstant.CREDIT)
                .loanTransactionType(LoanTransactionTypeConstant.INTEREST_TO_SUSPENSE)
                .build();

        loanTransactionEntityDao.saveRecord(creditInterestIncomeSuspenseAcct);

    }

    @Override
    public void moveFundFromMintLoanAccountToLoanSuspenseAccount(LoanRequestEntity loan, LoanTransactionEntity transaction) {

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(loan.getLoanAmount())
                .loanTransactionType(LoanTransactionTypeConstant.MINT_TO_SUSPENSE.name())
                .narration(constructLoanApprovalNarration(loan))
                .reference(transaction.getTransactionReference())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanApproval(request);

        if (msClientResponse.getData() != null) {
            transaction.setResponseCode(msClientResponse.getData().getResponseCode());
            transaction.setResponseMessage(msClientResponse.getData().getResponseMessage());
            transaction.setExternalReference(msClientResponse.getData().getBankOneReference());
            transaction = loanTransactionEntityDao.saveRecord(transaction);
        }

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), transaction.getTransactionReference(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Mint To Suspense funding failed", message);
        } else {
            transaction.setStatus(TransactionStatusConstant.SUCCESSFUL);
            loanTransactionEntityDao.saveRecord(transaction);

            LoanTransactionEntity creditCustomerAccount = LoanTransactionEntity.builder()
                    .loanRequest(loan)
                    .autoDebit(false)
                    .transactionAmount(loan.getLoanAmount())
                    .transactionReference(loanRequestEntityDao.generateLoanTransactionRef())
                    .status(TransactionStatusConstant.PENDING)
                    .transactionType(TransactionTypeConstant.CREDIT)
                    .loanTransactionType(LoanTransactionTypeConstant.SUSPENSE_TO_CUSTOMER)
                    .build();

            loanTransactionEntityDao.saveRecord(creditCustomerAccount);
        }
    }

    @Override
    public void moveFundFromLoanInterestReceivableAccountToInterestIncomeSuspenseAccount(LoanRequestEntity loan, LoanTransactionEntity transaction) {

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(loan.getRepaymentAmount().subtract(loan.getLoanAmount()))
                .loanTransactionType(LoanTransactionTypeConstant.INTEREST_TO_SUSPENSE.name())
                .narration(constructLoanApprovalNarration(loan))
                .reference(transaction.getTransactionReference())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanApproval(request);

        if (msClientResponse.getData() != null) {
            transaction.setResponseCode(msClientResponse.getData().getResponseCode());
            transaction.setResponseMessage(msClientResponse.getData().getResponseMessage());
            transaction.setExternalReference(msClientResponse.getData().getBankOneReference());
            transaction = loanTransactionEntityDao.saveRecord(transaction);
        }

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), transaction.getTransactionReference(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Interest To Suspense funding failed", message);
        } else {
            transaction.setStatus(TransactionStatusConstant.SUCCESSFUL);
            loanTransactionEntityDao.saveRecord(transaction);
         }
    }

    @Override
    public void moveFundFromLoanSuspenseAccountToCustomerAccount(LoanRequestEntity loan, LoanTransactionEntity transaction) {

        MintBankAccountEntity bankAccountEntity = mintBankAccountEntityDao.getRecordById(loan.getBankAccount().getId());

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(loan.getLoanAmount())
                .loanTransactionType(LoanTransactionTypeConstant.SUSPENSE_TO_CUSTOMER.name())
                .narration(constructLoanApprovalNarration(loan))
                .reference(transaction.getTransactionReference())
                .accountNumber(bankAccountEntity.getAccountNumber())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanApproval(request);

        if (msClientResponse.getData() != null) {
            transaction.setResponseCode(msClientResponse.getData().getResponseCode());
            transaction.setResponseMessage(msClientResponse.getData().getResponseMessage());
            transaction.setExternalReference(msClientResponse.getData().getBankOneReference());
            transaction = loanTransactionEntityDao.saveRecord(transaction);
        }

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), transaction.getTransactionReference(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Suspense To Customer funding failed", message);
        } else {
            transaction.setStatus(TransactionStatusConstant.SUCCESSFUL);
            loanTransactionEntityDao.saveRecord(transaction);

            //send disbursement email
        }
    }

    @Override
    public void processMintToSuspenseAccount() {
        List<LoanTransactionEntity> loanTransactions = loanTransactionEntityDao.getLoansPendingDisbursement(LoanTransactionTypeConstant.MINT_TO_SUSPENSE);

        for (LoanTransactionEntity transaction : loanTransactions) {
            LoanRequestEntity loan = loanRequestEntityDao.getRecordById(transaction.getLoanRequest().getId());
            moveFundFromMintLoanAccountToLoanSuspenseAccount(loan, transaction);
        }
    }

    @Override
    public void processInterestToSuspenseAccount() {
        List<LoanTransactionEntity> loanTransactions = loanTransactionEntityDao.getLoansPendingDisbursement(LoanTransactionTypeConstant.INTEREST_TO_SUSPENSE);

        for (LoanTransactionEntity transaction : loanTransactions) {
            LoanRequestEntity loan = loanRequestEntityDao.getRecordById(transaction.getLoanRequest().getId());
            moveFundFromLoanInterestReceivableAccountToInterestIncomeSuspenseAccount(loan, transaction);
        }
    }

    @Override
    public void processSuspenseAccountToCustomer() {
        List<LoanTransactionEntity> loanTransactions = loanTransactionEntityDao.getLoansPendingDisbursement(LoanTransactionTypeConstant.SUSPENSE_TO_CUSTOMER);

        for (LoanTransactionEntity transaction : loanTransactions) {
            LoanRequestEntity loan = loanRequestEntityDao.getRecordById(transaction.getLoanRequest().getId());
            moveFundFromLoanSuspenseAccountToCustomerAccount(loan, transaction);

        }
    }

    private String constructLoanApprovalNarration(LoanRequestEntity loanRequestEntity) {
        String narration = String.format("LA-%s %s", loanRequestEntity.getLoanId(), loanRequestEntity.getLoanType().name());
        if (narration.length() > 61) {
            return narration.substring(0, 60);
        }
        return narration;
    }
}
