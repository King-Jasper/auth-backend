package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsPlanEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsPlanTenorEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanTenorEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.AccountSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.models.MintSavingsGoalModel;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.utils.DateUtil;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jnwanya on
 * Wed, 01 Apr, 2020
 */
@FieldDefaults(makeFinal = true)
@AllArgsConstructor
@Named
public class GetSavingsGoalUseCaseImpl implements GetSavingsGoalUseCase {

    private SavingsPlanTenorEntityDao savingsPlanTenorEntityDao;
    private SavingsPlanEntityDao savingsPlanEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;
    private ApplicationProperty applicationProperty;

    @Override
    public SavingsGoalModel fromSavingsGoalEntityToModel(SavingsGoalEntity savingsGoalEntity) {
        SavingsPlanEntity savingsPlanEntity = savingsPlanEntityDao.getRecordById(savingsGoalEntity.getSavingsPlan().getId());
        SavingsPlanTenorEntity planTenorEntity = savingsPlanTenorEntityDao.getRecordById(savingsGoalEntity.getSavingsPlanTenor().getId());
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
                .availableBalance(computeAvailableBalance(savingsGoalEntity))
                .interestRate(planTenorEntity.getInterestRate())
                .build();
    }

    private BigDecimal computeAvailableBalance(SavingsGoalEntity savingsGoalEntity) {
         if(isCustomerGoalMatured(savingsGoalEntity)) {
             return savingsGoalEntity.getSavingsBalance().add(savingsGoalEntity.getAccruedInterest());
         }
        /*long remainingDays = savingsGoalEntity.getDateCreated().until(LocalDateTime.now(), ChronoUnit.DAYS);
        int minimumDaysForWithdrawal = applicationProperty.savingsMinimumNumberOfDaysForWithdrawal();
        if(remainingDays >= minimumDaysForWithdrawal) {
            SavingsPlanEntity savingsPlanEntity = savingsGoalEntity.getSavingsPlan();
            return savingsGoalEntity.getSavingsBalance().subtract(savingsPlanEntity.getMinimumBalance());
        }*/
        return BigDecimal.valueOf(0.00);
    }

   private boolean isCustomerGoalMatured(SavingsGoalEntity savingsGoalEntity) {
       if(savingsGoalEntity.getMaturityDate() == null) {
           return false;
       }
       LocalDateTime maturityDate = savingsGoalEntity.getMaturityDate();
       if(DateUtil.sameDay(LocalDateTime.now(), maturityDate)) {
           return true;
       }
       return maturityDate.isBefore(LocalDateTime.now());
   }
   private boolean isMintGoalMatured(SavingsGoalEntity savingsGoalEntity) {
       boolean matured = false;
       if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS) {
           if(applicationProperty.isProductionEnvironment()) {
               matured = BigDecimal.valueOf(1000.00).compareTo(savingsGoalEntity.getSavingsBalance()) <= 0;
           }else {
               matured = BigDecimal.valueOf(20.00).compareTo(savingsGoalEntity.getSavingsBalance()) <= 0;
           }
       }
       return matured;
   }

    public MintSavingsGoalModel fromSavingsGoalEntityToMintGoalModel(SavingsGoalEntity savingsGoalEntity) {
        boolean matured = isMintGoalMatured(savingsGoalEntity);
        BigDecimal availableBalance = matured ? savingsGoalEntity.getSavingsBalance().add(savingsGoalEntity.getAccruedInterest()) : BigDecimal.valueOf(0.00);
        return MintSavingsGoalModel.builder()
                .goalId(savingsGoalEntity.getGoalId())
                .name(savingsGoalEntity.getName())
                .savingsBalance(savingsGoalEntity.getSavingsBalance())
                .accruedInterest(savingsGoalEntity.getAccruedInterest())
                .availableBalance(availableBalance)
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
