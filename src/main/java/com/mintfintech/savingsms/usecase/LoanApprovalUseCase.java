package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.LoanTransactionEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.models.LoanModel;

public interface LoanApprovalUseCase {

    LoanModel approveLoanRequest(AuthenticatedUser authenticatedUser, String loanId, String reason, boolean approved);

    void moveFundFromMintLoanAccountToLoanSuspenseAccount(LoanRequestEntity loan, LoanTransactionEntity transaction);

    void moveFundFromLoanInterestReceivableAccountToInterestIncomeSuspenseAccount(LoanRequestEntity loan, LoanTransactionEntity transaction);

    void moveFundFromLoanSuspenseAccountToCustomerAccount(LoanRequestEntity loan, LoanTransactionEntity transaction);

    void processMintToSuspenseAccount();

    void processInterestToSuspenseAccount();

    void processSuspenseAccountToCustomer();
}
