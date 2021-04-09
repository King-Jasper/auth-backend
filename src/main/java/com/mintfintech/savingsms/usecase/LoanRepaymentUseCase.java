package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.LoanTransactionEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.models.LoanModel;

public interface LoanRepaymentUseCase {

    void dispatchEmailToCustomersWithPaymentDueInTwoDays();

    void dispatchEmailNotificationRepaymentOnDueDay();

    void checkDefaultedRepayment();

    void processPaymentOfDueRepayment();

    LoanModel repayment(AuthenticatedUser currentUser, double amount, String loanId);

    void moveFundFromLoanRecoverySuspenseAccountToMintLoanAccount(LoanRequestEntity loan, LoanTransactionEntity transaction);

    void moveFundFromLoanRecoverySuspenseAccountToInterestReceivableAccount(LoanRequestEntity loan, LoanTransactionEntity transaction);

    void moveFundFromInterestIncomeSuspenseAccountToInterestIncomeAccount(LoanRequestEntity loan, LoanTransactionEntity transaction);
}
