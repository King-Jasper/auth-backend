package com.mintfintech.savingsms.usecase.features.emergency_savings;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SavingsWithdrawalRequest;

public interface WithdrawEmergencySavingsUseCase {
    String withdrawalSavingsV2(AuthenticatedUser authenticatedUser, SavingsWithdrawalRequest withdrawalRequest);
}
