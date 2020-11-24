package com.mintfintech.savingsms.usecase.features.emergency_savings;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.models.EmergencySavingModel;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
/**
 * Created by jnwanya on
 * Sun, 01 Nov, 2020
 */
public interface GetEmergencySavingsUseCase {
    EmergencySavingModel getAccountEmergencySavings(AuthenticatedUser authenticatedUser);
}
