package com.mintfintech.savingsms.usecase.features.emergency_savings.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.request.EmergencySavingsCreationRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.emergency_savings.CreateEmergencySavingsUseCase;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import com.mintfintech.savingsms.utils.DateUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sun, 01 Nov, 2020
 */
@Slf4j
@Named
@AllArgsConstructor
public class CreateEmergencySavingsUseCaseImpl implements CreateEmergencySavingsUseCase {

    private final AppUserEntityDao appUserEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final SavingsGoalCategoryEntityDao savingsGoalCategoryEntityDao;
    private final SavingsPlanEntityDao savingsPlanEntityDao;
    private final SavingsPlanTenorEntityDao savingsPlanTenorEntityDao;
    private final SavingsGoalEntityDao savingsGoalEntityDao;
    private final GetSavingsGoalUseCase getSavingsGoalUseCase;


    @Override
    public SavingsGoalModel createSavingsGoal(AuthenticatedUser currentUser, EmergencySavingsCreationRequest creationRequest) {

        String emergencyCategoryCode = "10";
        int minimumDurationForInterest = 30;
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        Optional<SavingsGoalEntity> emergencyGoalOpt = savingsGoalEntityDao.findFirstSavingsByType(mintAccount, SavingsGoalTypeConstant.EMERGENCY_SAVINGS);
        if(emergencyGoalOpt.isPresent()) {
            SavingsGoalStatusConstant goalStatus = emergencyGoalOpt.get().getGoalStatus();
            if(goalStatus != SavingsGoalStatusConstant.COMPLETED && goalStatus != SavingsGoalStatusConstant.WITHDRAWN) {
                throw new BusinessLogicConflictException("Sorry, you have already created an emergency savings.");
            }
        }

        SavingsGoalCategoryEntity savingsGoalCategory = savingsGoalCategoryEntityDao.getCategoryByCode(emergencyCategoryCode);

        SavingsPlanEntity savingsPlan = savingsPlanEntityDao.getPlanByType(SavingsPlanTypeConstant.SAVINGS_TIER_ONE);
        SavingsPlanTenorEntity planTenor= savingsPlanTenorEntityDao.findSavingsPlanTenorForDuration(minimumDurationForInterest).get();

        BigDecimal targetAmount = BigDecimal.valueOf(creationRequest.getTargetAmount());
        BigDecimal fundingAmount = BigDecimal.valueOf(creationRequest.getFundingAmount());
        String goalName = creationRequest.getName();

        LocalDateTime startDate = creationRequest.getStartDate().atTime(LocalTime.now());
        SavingsFrequencyTypeConstant frequencyType = SavingsFrequencyTypeConstant.valueOf(creationRequest.getFrequency());
        LocalDateTime nextSavingsDate;
        LocalDateTime nearestHour = startDate.plusHours(1).withMinute(0).withSecond(0);
        if(frequencyType == SavingsFrequencyTypeConstant.DAILY) {
            nextSavingsDate = nearestHour.plusDays(1);
        }else if(frequencyType == SavingsFrequencyTypeConstant.WEEKLY) {
            nextSavingsDate = nearestHour.plusWeeks(1);
        }else {
            nextSavingsDate = nearestHour.plusMonths(1);
        }

        SavingsGoalStatusConstant statusConstant = DateUtil.sameDay(startDate, LocalDateTime.now()) ? SavingsGoalStatusConstant.ACTIVE: SavingsGoalStatusConstant.INACTIVE;

        SavingsGoalEntity savingsGoalEntity = SavingsGoalEntity.builder()
                .mintAccount(mintAccount)
                .creator(appUser)
                .goalCategory(savingsGoalCategory)
                .savingsGoalType(SavingsGoalTypeConstant.EMERGENCY_SAVINGS)
                .goalStatus(statusConstant)
                .goalId(savingsGoalEntityDao.generateSavingGoalId())
                .savingsAmount(fundingAmount)
                .targetAmount(targetAmount)
                .accruedInterest(BigDecimal.ZERO)
                .savingsBalance(BigDecimal.ZERO)
                .savingsFrequency(SavingsFrequencyTypeConstant.NONE)
                .autoSave(true)
                .savingsPlan(savingsPlan)
                .savingsPlanTenor(planTenor)
                .creationSource(SavingsGoalCreationSourceConstant.CUSTOMER)
                .name(goalName)
                .lockedSavings(false)
                .selectedDuration(minimumDurationForInterest)
                .nextAutoSaveDate(nextSavingsDate)
                .savingsStartDate(creationRequest.getStartDate())
                .build();

        savingsGoalEntity = savingsGoalEntityDao.saveRecord(savingsGoalEntity);
        log.info("Savings goal {} created with id: {}", goalName, savingsGoalEntity.getGoalId());

        return getSavingsGoalUseCase.fromSavingsGoalEntityToModel(savingsGoalEntity);
    }
}
