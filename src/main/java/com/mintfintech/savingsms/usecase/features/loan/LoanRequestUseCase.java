package com.mintfintech.savingsms.usecase.features.loan;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.models.LoanModel;

public interface LoanRequestUseCase {

    LoanModel loanRequest(AuthenticatedUser currentUser, double amount, String loanType, String creditAccountId);

    LoanModel paydayLoanRequest(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request);

}
