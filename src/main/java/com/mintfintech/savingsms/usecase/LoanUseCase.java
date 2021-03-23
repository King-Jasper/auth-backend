package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.models.LoanModel;

import java.math.BigDecimal;

public interface LoanUseCase {

    LoanModel approveLoanRequest(AuthenticatedUser authenticatedUser, String loanId);

    LoanModel rejectLoanRequest(AuthenticatedUser authenticatedUser, String loanId, String reason);

    BigDecimal getPendingRepaymentAmount(String loanId);

    LoanModel loanRequest(AuthenticatedUser currentUser, double amount, String loanType);
}
