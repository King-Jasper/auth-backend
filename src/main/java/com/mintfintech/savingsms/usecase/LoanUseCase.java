package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.models.LoanModel;

import java.math.BigDecimal;

public interface LoanUseCase {

    LoanModel approveLoanRequest(AuthenticatedUser authenticatedUser, String loanId, String reason, boolean approved);

    BigDecimal getPendingRepaymentAmount(String loanId);

    LoanModel loanRequest(AuthenticatedUser currentUser, double amount, String loanType);

    LoanModel repayment(AuthenticatedUser currentUser, double amount, String loanId);
}
