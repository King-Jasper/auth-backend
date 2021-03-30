package com.mintfintech.savingsms.usecase;

public interface LoanRepaymentUseCase {

    void dispatchEmailToCustomersWithPaymentDueInTwoDays();

    void dispatchEmailNotificationRepaymentOnDueDay();

    void checkDefaultedRepayment();

    void processPaymentOfDueRepayment();
}
