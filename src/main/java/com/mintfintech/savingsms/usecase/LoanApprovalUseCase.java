package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.LoanApprovalEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.models.LoanModel;

public interface LoanApprovalUseCase {

    LoanModel approveLoanRequest(AuthenticatedUser authenticatedUser, String loanId, String reason, boolean approved);

    void creditLoanSuspenseAccount(LoanRequestEntity loan, LoanApprovalEntity approval);

    void creditInterestIncomeSuspenseAccount(LoanRequestEntity loan, LoanApprovalEntity approval);

    void creditCustomerAccount(LoanRequestEntity loan, LoanApprovalEntity approval);

    void processMintToSuspenseAccount();

    void processInterestToSuspenseAccount();

    void processSuspenseAccountToCustomer();
}
