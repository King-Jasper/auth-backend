package com.mintfintech.savingsms.usecase.features.emergency_savings.impl;

import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.features.emergency_savings.GetEmergencySavingsUseCase;
import com.mintfintech.savingsms.usecase.models.EmergencySavingModel;
import com.mintfintech.savingsms.usecase.models.EmergencySavingModelV2;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        if(emergencySaving.getGoalStatus() == SavingsGoalStatusConstant.COMPLETED || emergencySaving.getGoalStatus() == SavingsGoalStatusConstant.WITHDRAWN) {
            return EmergencySavingModel.builder().exist(false).build();
        }
        SavingsGoalModel goalModel = getSavingsGoalUseCase.fromSavingsGoalEntityToModel(emergencySaving);
        //goalModel.setInterestRate(0.0);
        return EmergencySavingModel.builder()
                .exist(true)
                .savingsGoal(goalModel)
                .build();
    }

    @Override
    public EmergencySavingModelV2 getAccountEmergencySavingsV2(AuthenticatedUser authenticatedUser) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        List<SavingsGoalEntity> savingsGoalList = savingsGoalEntityDao.getAllSavingsByType(accountEntity, SavingsGoalTypeConstant.EMERGENCY_SAVINGS);
        List<SavingsGoalModel> goalModelList = savingsGoalList.stream().map(getSavingsGoalUseCase::fromSavingsGoalEntityToModel).collect(Collectors.toList());

        return EmergencySavingModelV2.builder()
                .exist(!goalModelList.isEmpty())
                .savingsGoals(goalModelList)
                .build();
    }
}
