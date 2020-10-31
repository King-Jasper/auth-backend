package com.mintfintech.savingsms.usecase.features.roundup_savings;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.RoundUpSavingResponse;

/**
 * Created by jnwanya on
 * Sat, 31 Oct, 2020
 */
public interface UpdateRoundUpSavingsUseCase {
    RoundUpSavingResponse updateRoundUpType(AuthenticatedUser authenticatedUser, Long roundUpSetUpId, String roundUpType);
    RoundUpSavingResponse updateRoundUpSavingsStatus(AuthenticatedUser authenticatedUser, Long roundUpSetUpId, boolean active);
    void deleteDeactivatedRoundUpSavingsWithZeroBalance();
}
