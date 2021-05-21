package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SavingsGoalCreationRequest;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
public interface CreateSavingsGoalUseCase {
    //SavingsGoalEntity createDefaultSavingsGoal(MintAccountEntity mintAccountEntity, AppUserEntity appUserEntity);
    SavingsGoalModel  createNewSavingsGoal(AuthenticatedUser currentUser, SavingsGoalCreationRequest goalCreationRequest);

    void runInterestUpdate();
}
