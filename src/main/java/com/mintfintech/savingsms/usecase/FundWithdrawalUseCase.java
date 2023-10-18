package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SavingsWithdrawalRequest;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 06 Apr, 2020
 */
public interface FundWithdrawalUseCase {
    BigDecimal unlockSavings(AuthenticatedUser authenticatedUser, String goalId, String phoneNumber);
    String withdrawalSavings(AuthenticatedUser authenticatedUser, SavingsWithdrawalRequest withdrawalRequest);
    void processInterestWithdrawalToSuspenseAccount();
    void processSavingsWithdrawalToSuspenseAccount();
    void processSuspenseFundDisbursementToCustomer();
    void processWithHoldingTaxCharge();
}
