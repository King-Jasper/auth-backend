package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.models.LoanModel;

public interface LoanUseCase {

    LoanModel approveLoanRequest(AuthenticatedUser authenticatedUser, String loanId, String reason, boolean approved);

    LoanModel loanRequest(AuthenticatedUser currentUser, double amount, String loanType);

    LoanModel repayment(AuthenticatedUser currentUser, double amount, String loanId);

    LoanModel getLoanTransactions(String loanId);

    void updateCustomerRating(AppUserEntity currentUser);
}
