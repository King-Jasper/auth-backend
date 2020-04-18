package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsPlanEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.AccountSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.models.MintSavingsGoalModel;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jnwanya on
 * Wed, 01 Apr, 2020
 */
@AllArgsConstructor
@Named
public class GetSavingsGoalUseCaseImpl implements GetSavingsGoalUseCase {

    private SavingsPlanEntityDao savingsPlanEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;

    @Override
    public SavingsGoalModel fromSavingsGoalEntityToModel(SavingsGoalEntity savingsGoalEntity) {
        SavingsPlanEntity savingsPlanEntity = savingsPlanEntityDao.getRecordById(savingsGoalEntity.getSavingsPlan().getId());
        String maturityDate = "";
        String nextSavingsDate = "";
        if(savingsGoalEntity.getMaturityDate() != null) {
            maturityDate = savingsGoalEntity.getMaturityDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if(savingsGoalEntity.getNextAutoSaveDate() != null) {
            nextSavingsDate = savingsGoalEntity.getNextAutoSaveDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        return SavingsGoalModel.builder()
                .goalId(savingsGoalEntity.getGoalId())
                .name(savingsGoalEntity.getName())
                .autoSaveEnabled(savingsGoalEntity.isAutoSave())
                .savingsBalance(savingsGoalEntity.getSavingsBalance())
                .savingsAmount(savingsGoalEntity.getSavingsAmount())
                .targetAmount(savingsGoalEntity.getTargetAmount())
                .accruedInterest(savingsGoalEntity.getAccruedInterest())
                .savingFrequency(savingsGoalEntity.getSavingsFrequency() != null ? savingsGoalEntity.getSavingsFrequency().name() : "")
                .savingPlanName(savingsPlanEntity.getPlanName().getName())
                .maturityDate(maturityDate)
                .nextSavingsDate(nextSavingsDate)
                .startDate(savingsGoalEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE))
                .categoryCode(savingsGoalEntity.getGoalCategory().getCode())
                .currentStatus(savingsGoalEntity.getGoalStatus().name())
                .build();
    }

    public MintSavingsGoalModel fromSavingsGoalEntityToMintGoalModel(SavingsGoalEntity savingsGoalEntity) {
        boolean matured = false;
        if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS) {
            matured = BigDecimal.valueOf(1000.00).compareTo(savingsGoalEntity.getSavingsBalance()) <= 0;
        }
        return MintSavingsGoalModel.builder()
                .goalId(savingsGoalEntity.getGoalId())
                .name(savingsGoalEntity.getName())
                .savingsBalance(savingsGoalEntity.getSavingsBalance())
                .accruedInterest(savingsGoalEntity.getAccruedInterest())
                .matured(matured).build();
    }

    @Override
    public SavingsGoalModel getSavingsGoalByGoalId(AuthenticatedUser authenticatedUser, String goalId) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        SavingsGoalEntity savingsGoal = savingsGoalEntityDao.findSavingGoalByAccountAndGoalId(accountEntity, goalId)
                .orElseThrow(() -> new BadRequestException("Invalid savings goal Id."));
        return fromSavingsGoalEntityToModel(savingsGoal);
    }

    @Override
    public List<SavingsGoalModel> getSavingsGoalList(MintAccountEntity mintAccountEntity) {
        List<SavingsGoalModel> savingsGoalList = savingsGoalEntityDao.getAccountSavingGoals(mintAccountEntity)
                .stream()
                .filter(savingsGoalEntity -> savingsGoalEntity.getSavingsGoalType() != SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS)
                .map(this::fromSavingsGoalEntityToModel)
                .collect(Collectors.toList());
        return savingsGoalList;
    }

    @Override
    public List<SavingsGoalModel> getSavingsGoalList(AuthenticatedUser authenticatedUser) {
        MintAccountEntity mintAccountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        return getSavingsGoalList(mintAccountEntity);
    }

    @Override
    public AccountSavingsGoalResponse getAccountSavingsGoals(AuthenticatedUser authenticatedUser) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        List<MintSavingsGoalModel> mintGoalsList = new ArrayList<>();
        List<SavingsGoalModel> savingsGoalList = new ArrayList<>();
        List<SavingsGoalEntity> savingsGoalEntityList = savingsGoalEntityDao.getAccountSavingGoals(accountEntity);
        for(SavingsGoalEntity savingsGoalEntity : savingsGoalEntityList) {
            if(savingsGoalEntity.getCreationSource() == SavingsGoalCreationSourceConstant.CUSTOMER) {
                 savingsGoalList.add(fromSavingsGoalEntityToModel(savingsGoalEntity));
            }else {
               mintGoalsList.add(fromSavingsGoalEntityToMintGoalModel(savingsGoalEntity));
            }
        }
        return AccountSavingsGoalResponse.builder()
                .customerGoals(savingsGoalList)
                .mintGoals(mintGoalsList)
                .build();
    }
}
