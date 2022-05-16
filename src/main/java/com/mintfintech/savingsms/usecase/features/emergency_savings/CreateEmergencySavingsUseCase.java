package com.mintfintech.savingsms.usecase.features.emergency_savings;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.EmergencySavingsCreationRequest;
import com.mintfintech.savingsms.usecase.models.EmergencySavingModel;
import com.mintfintech.savingsms.usecase.models.EmergencySavingsModel;

/**
 * Created by jnwanya on
 * Sun, 01 Nov, 2020
 */
public interface CreateEmergencySavingsUseCase {
    EmergencySavingModel createSavingsGoal(AuthenticatedUser currentUser, EmergencySavingsCreationRequest creationRequest);
    EmergencySavingsModel createSavingsGoalV2(AuthenticatedUser currentUser, EmergencySavingsCreationRequest creationRequest);
}
