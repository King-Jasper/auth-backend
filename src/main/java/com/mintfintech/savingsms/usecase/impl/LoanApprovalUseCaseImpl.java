package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.LoanApprovalEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanTransactionEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.LoanApprovalEntity;
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
    private final LoanApprovalEntityDao loanApprovalEntityDao;

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

            startFundDisbursement(loanRequestEntity);

        } else {
            loanRequestEntity.setApprovalStatus(ApprovalStatusConstant.REJECTED);
            loanRequestEntity.setApproveByName(authenticatedUser.getUsername());
            loanRequestEntity.setApproveByUserId(authenticatedUser.getUserId());
            loanRequestEntity.setRejectionReason(reason);

            loanRequestEntityDao.saveRecord(loanRequestEntity);
        }

        return getLoansUseCase.toLoanModel(loanRequestEntity);
    }

    private void startFundDisbursement(LoanRequestEntity loan) {

        LoanApprovalEntity loanApprovalEntity = LoanApprovalEntity.builder()
                .loanRequest(loan)
                .loanTransactionType(LoanTransactionTypeConstant.PENDING_MINT_TO_SUSPENSE)
                .build();
        loanApprovalEntityDao.saveRecord(loanApprovalEntity);
    }

    @Override
    public void creditLoanSuspenseAccount(LoanRequestEntity loan, LoanApprovalEntity approval) {

        String ref = loanRequestEntityDao.generateLoanTransactionRef();

        approval.setLoanSuspenseCreditReference(ref);
        approval.setLoanTransactionType(LoanTransactionTypeConstant.PROCESSING_MINT_TO_SUSPENSE);
        loanApprovalEntityDao.saveAndFlush(approval);

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(loan.getLoanAmount())
                .loanTransactionType(LoanTransactionTypeConstant.MINT_TO_SUSPENSE.name())
                .narration(constructLoanApprovalNarration(loan, ref))
                .reference(ref)
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanApproval(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), ref, msClientResponse.getMessage());
            systemIssueLogService.logIssue("Loan Approval Issue", "Mint To Suspense funding failed", message);
            approval.setLoanTransactionType(LoanTransactionTypeConstant.FAILED_MINT_TO_SUSPENSE);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            approval.setLoanSuspenseCreditResponseCode(responseCBS.getResponseCode());
            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                approval.setLoanTransactionType(LoanTransactionTypeConstant.PENDING_INTEREST_TO_SUSPENSE);
            } else {
                approval.setLoanTransactionType(LoanTransactionTypeConstant.FAILED_MINT_TO_SUSPENSE);
                String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), ref, msClientResponse.getMessage());
                systemIssueLogService.logIssue("Loan Approval Issue", "Mint To Suspense funding failed", message);
            }
        }

        loanApprovalEntityDao.saveRecord(approval);
    }

    @Override
    public void creditInterestIncomeSuspenseAccount(LoanRequestEntity loan, LoanApprovalEntity approval) {

        String ref = loanRequestEntityDao.generateLoanTransactionRef();

        approval.setLoanIncomeSuspenseCreditReference(ref);
        approval.setLoanTransactionType(LoanTransactionTypeConstant.PROCESSING_INTEREST_TO_SUSPENSE);
        loanApprovalEntityDao.saveAndFlush(approval);

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(loan.getLoanInterest())
                .loanTransactionType(LoanTransactionTypeConstant.INTEREST_TO_SUSPENSE.name())
                .narration(constructLoanApprovalNarration(loan, ref))
                .reference(ref)
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanApproval(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), ref, msClientResponse.getMessage());
            systemIssueLogService.logIssue("Loan Approval Issue", "Interest To Suspense funding failed", message);
            approval.setLoanTransactionType(LoanTransactionTypeConstant.FAILED_INTEREST_TO_SUSPENSE);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            approval.setLoanIncomeSuspenseCreditCode(responseCBS.getResponseCode());
            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                approval.setLoanTransactionType(LoanTransactionTypeConstant.PENDING_SUSPENSE_TO_CUSTOMER);
            } else {
                approval.setLoanTransactionType(LoanTransactionTypeConstant.FAILED_INTEREST_TO_SUSPENSE);
                String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), ref, msClientResponse.getMessage());
                systemIssueLogService.logIssue("Loan Approval Issue", "Interest To Suspense funding failed", message);
            }
        }
        loanApprovalEntityDao.saveRecord(approval);
    }

    @Override
    public void creditCustomerAccount(LoanRequestEntity loan, LoanApprovalEntity approval) {

        String ref = loanRequestEntityDao.generateLoanTransactionRef();

        LoanTransactionEntity transaction = LoanTransactionEntity.builder()
                .transactionType(TransactionTypeConstant.CREDIT)
                .transactionReference(ref)
                .transactionAmount(loan.getLoanAmount())
                .autoDebit(false)
                .loanRequest(loan)
                .status(TransactionStatusConstant.PENDING)
                .build();

        transaction = loanTransactionEntityDao.saveRecord(transaction);
        approval.setDisbursementTransaction(transaction);
        approval.setLoanTransactionType(LoanTransactionTypeConstant.PROCESSING_SUSPENSE_TO_CUSTOMER);
        loanApprovalEntityDao.saveAndFlush(approval);

        MintBankAccountEntity bankAccountEntity = mintBankAccountEntityDao.getRecordById(loan.getBankAccount().getId());

        LoanTransactionRequestCBS request = LoanTransactionRequestCBS.builder()
                .loanId(loan.getLoanId())
                .amount(loan.getLoanAmount())
                .loanTransactionType(LoanTransactionTypeConstant.SUSPENSE_TO_CUSTOMER.name())
                .narration(constructLoanApprovalNarration(loan, ref))
                .reference(ref)
                .accountNumber(bankAccountEntity.getAccountNumber())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processLoanApproval(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), transaction.getTransactionReference(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Loan Approval Issue", "Suspense To Customer funding failed", message);
            transaction.setStatus(TransactionStatusConstant.FAILED);
            approval.setLoanTransactionType(LoanTransactionTypeConstant.FAILED_SUSPENSE_TO_CUSTOMER);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            transaction.setResponseCode(responseCBS.getResponseCode());
            transaction.setResponseMessage(responseCBS.getResponseMessage());
            transaction.setExternalReference(responseCBS.getBankOneReference());

            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                approval.setLoanTransactionType(LoanTransactionTypeConstant.PROCESSED);
                transaction.setStatus(TransactionStatusConstant.SUCCESSFUL);

                loan.setApprovalStatus(ApprovalStatusConstant.DISBURSED);
                loanRequestEntityDao.saveRecord(loan);

                //send disbursement email
            } else {
                approval.setLoanTransactionType(LoanTransactionTypeConstant.FAILED_SUSPENSE_TO_CUSTOMER);
                transaction.setStatus(TransactionStatusConstant.FAILED);
                String message = String.format("Loan Id: %s; transaction Id: %s ; message: %s", loan.getLoanId(), transaction.getTransactionReference(), msClientResponse.getMessage());
                systemIssueLogService.logIssue("Loan Approval Issue", "Suspense To Customer funding failed", message);
            }
        }
        loanTransactionEntityDao.saveRecord(transaction);
        loanApprovalEntityDao.saveRecord(approval);
    }

    @Override
    public void processMintToSuspenseAccount() {
        List<LoanApprovalEntity> loanApprovals = loanApprovalEntityDao.getPendingMintToSuspenseTransaction();
        for (LoanApprovalEntity approval : loanApprovals) {
            LoanRequestEntity loan = loanRequestEntityDao.getRecordById(approval.getLoanRequest().getId());
            creditLoanSuspenseAccount(loan, approval);
        }
    }

    @Override
    public void processInterestToSuspenseAccount() {
        List<LoanApprovalEntity> loanApprovals = loanApprovalEntityDao.getPendingInterestToSuspenseTransaction();

        for (LoanApprovalEntity approval : loanApprovals) {
            LoanRequestEntity loan = loanRequestEntityDao.getRecordById(approval.getLoanRequest().getId());
            creditInterestIncomeSuspenseAccount(loan, approval);
        }
    }

    @Override
    public void processSuspenseAccountToCustomer() {
        List<LoanApprovalEntity> loanApprovals = loanApprovalEntityDao.getPendingSuspenseToCustomerTransaction();

        for (LoanApprovalEntity approval : loanApprovals) {
            LoanRequestEntity loan = loanRequestEntityDao.getRecordById(approval.getLoanRequest().getId());
            creditCustomerAccount(loan, approval);

        }
    }

    private String constructLoanApprovalNarration(LoanRequestEntity loanRequestEntity, String reference) {
        String narration = String.format("LA-%s %s", loanRequestEntity.getLoanId(), reference);
        if (narration.length() > 61) {
            return narration.substring(0, 60);
        }
        return narration;
    }
}
