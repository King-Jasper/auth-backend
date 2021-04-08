package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.models.LoanModel;

public interface LoanUseCase {

    LoanModel approveLoanRequest(AuthenticatedUser authenticatedUser, String loanId, String reason, boolean approved);

    LoanModel loanRequest(AuthenticatedUser currentUser, double amount, String loanType);

    LoanModel paydayLoanRequest(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request);

    LoanModel getLoanTransactions(String loanId);

}
