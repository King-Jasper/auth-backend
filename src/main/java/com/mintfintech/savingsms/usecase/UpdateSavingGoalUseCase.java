package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SavingsFrequencyUpdateRequest;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
public interface UpdateSavingGoalUseCase {
    SavingsGoalModel updateSavingFrequency(AuthenticatedUser currentUser, SavingsFrequencyUpdateRequest autoSaveRequest);
    void cancelSavingFrequency(AuthenticatedUser currentUser, String goalId);
}
