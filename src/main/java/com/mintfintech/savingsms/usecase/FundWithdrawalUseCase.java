package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SavingsWithdrawalRequest;

/**
 * Created by jnwanya on
 * Mon, 06 Apr, 2020
 */
public interface FundWithdrawalUseCase {
    String withdrawalSavings(AuthenticatedUser authenticatedUser, SavingsWithdrawalRequest withdrawalRequest);
    void processInterestWithdrawalToSuspenseAccount();
    void processSavingsWithdrawalToSuspenseAccount();
    void processSuspenseFundDisbursementToCustomer();
}
