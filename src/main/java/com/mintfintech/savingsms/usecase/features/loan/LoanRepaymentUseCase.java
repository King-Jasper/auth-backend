package com.mintfintech.savingsms.usecase.features.loan;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.events.incoming.AccountCreditEvent;
import com.mintfintech.savingsms.usecase.models.LoanModel;

public interface LoanRepaymentUseCase {

    void dispatchEmailToCustomersWithPaymentDueInTwoDays();

    void checkDefaultedRepayment();

    void processPaymentOfOverDueRepayment(AccountCreditEvent accountCreditEvent);

    LoanModel repayment(AuthenticatedUser currentUser, double amount, String loanId);

    void processLoanRecoverySuspenseAccountToMintLoanAccount();

    void processInterestIncomeSuspenseAccountToInterestIncomeAccount();

}
