package com.mintfintech.savingsms.usecase.features.roundup_savings;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.RoundUpSavingSetUpRequest;
import com.mintfintech.savingsms.usecase.data.response.RoundUpSavingResponse;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
/**
 * Created by jnwanya on
 * Sat, 31 Oct, 2020
 */
public interface CreateRoundUpSavingsUseCase {
    RoundUpSavingResponse setupRoundUpSavings(AuthenticatedUser authenticatedUser, RoundUpSavingSetUpRequest roundUpSavingSetUpRequest);
}
