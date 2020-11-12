package com.mintfintech.savingsms.usecase.features.emergency_savings.impl;

import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.features.emergency_savings.GetEmergencySavingsUseCase;
import com.mintfintech.savingsms.usecase.models.EmergencySavingModel;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sun, 01 Nov, 2020
 */
@Named
@AllArgsConstructor
public class GetEmergencySavingsUseCaseImpl implements GetEmergencySavingsUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final GetSavingsGoalUseCase getSavingsGoalUseCase;
    private final SavingsGoalEntityDao savingsGoalEntityDao;

    @Override
    public EmergencySavingModel getAccountEmergencySavings(AuthenticatedUser authenticatedUser) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        Optional<SavingsGoalEntity> emergencySavingOpt = savingsGoalEntityDao.findFirstSavingsByType(accountEntity, SavingsGoalTypeConstant.EMERGENCY_SAVINGS);
        if(!emergencySavingOpt.isPresent()) {
            return EmergencySavingModel.builder().exist(false).build();
        }
        SavingsGoalEntity emergencySaving = emergencySavingOpt.get();
        SavingsGoalModel goalModel = getSavingsGoalUseCase.fromSavingsGoalEntityToModel(emergencySaving);
        //goalModel.setInterestRate(0.0);
        return EmergencySavingModel.builder()
                .exist(true)
                .savingsGoal(goalModel)
                .build();
    }
}
