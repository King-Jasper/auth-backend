package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
public interface ChangeSavingsPlanUseCase {
    SavingsGoalModel changePlan(AuthenticatedUser authenticatedUser, String goalId, String planId);
}
