package com.mintfintech.savingsms.usecase.features.emergency_savings.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.request.EmergencySavingsCreationRequest;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.emergency_savings.CreateEmergencySavingsUseCase;
import com.mintfintech.savingsms.usecase.models.EmergencySavingModel;
import com.mintfintech.savingsms.usecase.models.EmergencySavingsModel;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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
    private final MintBankAccountEntityDao bankAccountEntityDao;
    private final FundSavingsGoalUseCase fundSavingsGoalUseCase;
    private final ApplicationProperty applicationProperty;


    @Transactional
    @Override
    public EmergencySavingModel createSavingsGoal(AuthenticatedUser currentUser, EmergencySavingsCreationRequest creationRequest) {

        String emergencyCategoryCode = "10";
        int minimumDurationForInterest = 30;
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());
        MintBankAccountEntity debitAccount = bankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccount, BankAccountTypeConstant.CURRENT);

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
        LocalDateTime nextSavingsDate = null;
        SavingsFrequencyTypeConstant frequencyType = SavingsFrequencyTypeConstant.NONE;
        SavingsGoalStatusConstant statusConstant = SavingsGoalStatusConstant.ACTIVE;
        if(creationRequest.isAutoDebit()) {
            if(creationRequest.getStartDate() == null || StringUtils.isEmpty(creationRequest.getFrequency())) {
                throw new BadRequestException("Start date and frequency is required.");
            }
            LocalDateTime startDate = creationRequest.getStartDate().atTime(LocalTime.now());
            frequencyType = SavingsFrequencyTypeConstant.valueOf(creationRequest.getFrequency());
            nextSavingsDate = startDate.plusHours(1).withMinute(0).withSecond(0);
            //statusConstant = DateUtil.sameDay(startDate, LocalDateTime.now()) ? SavingsGoalStatusConstant.ACTIVE: SavingsGoalStatusConstant.INACTIVE;
            statusConstant = SavingsGoalStatusConstant.ACTIVE;
        }else {
            if(debitAccount.getAvailableBalance().compareTo(fundingAmount) < 0) {
                throw new BusinessLogicConflictException("You have insufficient balance for fund your savings goal.");
            }
            if(fundingAmount.compareTo(targetAmount) > 0) {
                throw new BadRequestException("Amount to be funded is already greater than target amount. Please increase target amount.");
            }
        }

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
                .savingsFrequency(frequencyType)
                .autoSave(creationRequest.isAutoDebit())
                .savingsPlan(savingsPlan)
                .savingsPlanTenor(planTenor)
                .interestRate(planTenor.getInterestRate())
                .creationSource(SavingsGoalCreationSourceConstant.CUSTOMER)
                .name(goalName)
                .lockedSavings(false)
                .selectedDuration(minimumDurationForInterest)
                .nextAutoSaveDate(nextSavingsDate)
                .savingsStartDate(creationRequest.getStartDate())
                .build();

        savingsGoalEntity = savingsGoalEntityDao.saveRecord(savingsGoalEntity);
        log.info("Savings goal {} created with id: {}", goalName, savingsGoalEntity.getGoalId());

        if(!creationRequest.isAutoDebit()) {
            SavingsGoalFundingResponse fundingResponse = fundSavingsGoalUseCase.fundSavingGoal(debitAccount, appUser,  savingsGoalEntity, fundingAmount);
            if(!fundingResponse.getResponseCode().equalsIgnoreCase("00")) {
                throw new BusinessLogicConflictException("Sorry, temporary unable to fund your saving goal. Please try again later.");
            }
        }

        return EmergencySavingModel.builder()
                .exist(true)
                .savingsGoal(getSavingsGoalUseCase.fromSavingsGoalEntityToModel(savingsGoalEntity))
                .build();
    }

    @Override
    public EmergencySavingsModel createSavingsGoalV2(AuthenticatedUser currentUser, EmergencySavingsCreationRequest creationRequest) {

        String emergencyCategoryCode = applicationProperty.getEmergencyCategoryCode();
        int minimumDurationForInterest = 30;
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        if (mintAccount.getAccountType() == AccountTypeConstant.ENTERPRISE || mintAccount.getAccountType() == AccountTypeConstant.INCORPORATED_TRUSTEE) {
            throw new BusinessLogicConflictException("Sorry, this feature is not currently supported for your account type.");
        }

        MintBankAccountEntity debitAccount = bankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccount, BankAccountTypeConstant.CURRENT);

        BigDecimal targetAmount = BigDecimal.valueOf(creationRequest.getTargetAmount());
        BigDecimal fundingAmount = BigDecimal.valueOf(creationRequest.getFundingAmount());
        String goalName = creationRequest.getName();
        LocalDateTime nextSavingsDate = null;
        SavingsFrequencyTypeConstant frequencyType = SavingsFrequencyTypeConstant.NONE;
        SavingsGoalStatusConstant statusConstant = SavingsGoalStatusConstant.ACTIVE;

        SavingsGoalCategoryEntity savingsGoalCategory = savingsGoalCategoryEntityDao.getCategoryByCode(emergencyCategoryCode);
        SavingsPlanEntity savingsPlan = savingsPlanEntityDao.getPlanByType(SavingsPlanTypeConstant.SAVINGS_TIER_ONE);
        SavingsPlanTenorEntity planTenor = savingsPlanTenorEntityDao.findSavingsPlanTenorForDuration(minimumDurationForInterest).get();

        Optional<SavingsGoalEntity> savingsGoalOptional = savingsGoalEntityDao.findGoalByNameAndPlanAndAccountAndType(creationRequest.getName(),
                savingsPlan, mintAccount, SavingsGoalTypeConstant.EMERGENCY_SAVINGS);
        if (savingsGoalOptional.isPresent()) {
            throw new BusinessLogicConflictException("Emergency savings goal with name "+ creationRequest.getName() + " exists already.");
        }
        /*
        if (fundingAmount.compareTo(targetAmount) > 0) {
            throw new BadRequestException("Amount to be funded is already greater than target amount. Please increase target amount.");
        }*/
        if (creationRequest.isAutoDebit()) {
            LocalDateTime startDate = creationRequest.getStartDate().atTime(LocalTime.now());
            if (creationRequest.getStartDate() == null || StringUtils.isEmpty(creationRequest.getFrequency())) {
                throw new BadRequestException("Start date and savings frequency are required.");
            }
            if(creationRequest.getStartDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("Start date cannot be before current date.");
            }
            frequencyType = SavingsFrequencyTypeConstant.valueOf(creationRequest.getFrequency());
            nextSavingsDate = startDate.plusHours(1).withMinute(0).withSecond(0);
        } else {
            if (debitAccount.getAvailableBalance().compareTo(fundingAmount) < 0) {
                throw new BusinessLogicConflictException("You have insufficient balance to fund your savings goal.");
            }
        }

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
                .savingsFrequency(frequencyType)
                .autoSave(creationRequest.isAutoDebit())
                .savingsPlan(savingsPlan)
                .savingsPlanTenor(planTenor)
                .creationSource(SavingsGoalCreationSourceConstant.CUSTOMER)
                .name(goalName)
                .lockedSavings(false)
                .nextAutoSaveDate(nextSavingsDate)
                .savingsStartDate(creationRequest.getStartDate())
                .build();
        savingsGoalEntity = savingsGoalEntityDao.saveRecord(savingsGoalEntity);

        if (!creationRequest.isAutoDebit()) {
            SavingsGoalFundingResponse fundingResponse = fundSavingsGoalUseCase.fundSavingGoal(debitAccount, appUser, savingsGoalEntity, fundingAmount);
            if (!fundingResponse.getResponseCode().equalsIgnoreCase("00")) {
                savingsGoalEntity.setRecordStatus(RecordStatusConstant.DELETED);
                savingsGoalEntityDao.saveRecord(savingsGoalEntity);
                throw new BusinessLogicConflictException("Sorry, temporary unable to fund your saving goal. Please try again later.");
            }
        } else {
            if (creationRequest.getStartDate().equals(LocalDate.now())) {
                SavingsGoalFundingResponse fundingResponse = fundSavingsGoalUseCase.fundSavingGoal(debitAccount, appUser, savingsGoalEntity, fundingAmount);
                if (!fundingResponse.getResponseCode().equalsIgnoreCase("00")) {
                    savingsGoalEntity.setRecordStatus(RecordStatusConstant.DELETED);
                    savingsGoalEntityDao.saveRecord(savingsGoalEntity);
                    throw new BusinessLogicConflictException("Sorry, temporary unable to fund your saving goal. Please try again later.");
                }
                savingsGoalEntity.setNextAutoSaveDate(getNextSavingsDate(frequencyType, nextSavingsDate));
                savingsGoalEntityDao.saveRecord(savingsGoalEntity);
            }
        }

        SavingsGoalModel savingsGoal = getSavingsGoalUseCase.fromSavingsGoalEntityToModel(savingsGoalEntity);
        List<SavingsGoalModel> savingsGoalModelList = new ArrayList<>();
        savingsGoalModelList.add(savingsGoal);
        return EmergencySavingsModel.builder()
                .exist(true)
                .savingsGoals(savingsGoalModelList)
                .build();
    }

    private LocalDateTime getNextSavingsDate(SavingsFrequencyTypeConstant frequencyType, LocalDateTime nextSavingsDate) {
        nextSavingsDate = nextSavingsDate.withNano(0).withSecond(0).withMinute(0);
        LocalDateTime newNextSavingsDate = nextSavingsDate.plusDays(1);
        if (frequencyType.equals(SavingsFrequencyTypeConstant.WEEKLY)) {
            newNextSavingsDate = nextSavingsDate.plusWeeks(1);
        } else if (frequencyType.equals(SavingsFrequencyTypeConstant.MONTHLY)) {
            newNextSavingsDate = nextSavingsDate.plusMonths(1);
        }
        return newNextSavingsDate;
    }
}
