package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.PagedResponse;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.CreateSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.request.EmergencySavingsCreationRequest;
import com.mintfintech.savingsms.usecase.data.request.SavingsGoalCreationRequest;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.features.referral_savings.CreateReferralRewardUseCase;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;

import javax.inject.Named;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@FieldDefaults(makeFinal = true)
@Slf4j
@Named
@AllArgsConstructor
public class CreateSavingsGoalUseCaseImpl implements CreateSavingsGoalUseCase {

    private AppUserEntityDao appUserEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private SavingsPlanTenorEntityDao savingsPlanTenorEntityDao;
    private SavingsPlanEntityDao savingsPlanEntityDao;
    private SavingsGoalCategoryEntityDao savingsGoalCategoryEntityDao;
    private SavingsInterestEntityDao savingsInterestEntityDao;
    private TierLevelEntityDao tierLevelEntityDao;
    private GetSavingsGoalUseCase getSavingsGoalUseCase;
    private FundSavingsGoalUseCase fundSavingsGoalUseCase;

    /*@Override
    public SavingsGoalEntity createDefaultSavingsGoal(MintAccountEntity mintAccountEntity, AppUserEntity appUserEntity) {
        SavingsGoalCategoryEntity goalCategoryEntity = savingsGoalCategoryEntityDao.findCategoryByCode("08").get();
        SavingsPlanEntity savingsPlanEntity = savingsPlanEntityDao.getPlanByType(SavingsPlanTypeConstant.SAVINGS_TIER_ONE);
        SavingsPlanTenorEntity planTenorEntity = savingsPlanTenorEntityDao.getLeastDurationOnSavingsPlan(savingsPlanEntity);
        SavingsGoalEntity savingsGoalEntity  = SavingsGoalEntity.builder()
                .savingsGoalType(SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS)
                .savingsFrequency(SavingsFrequencyTypeConstant.NONE)
                .savingsPlan(savingsPlanEntity)
                .autoSave(false)
                .creationSource(SavingsGoalCreationSourceConstant.MINT)
                .goalStatus(SavingsGoalStatusConstant.ACTIVE)
                .targetAmount(BigDecimal.ZERO)
                .savingsBalance(BigDecimal.ZERO)
                .accruedInterest(BigDecimal.ZERO)
                .mintAccount(mintAccountEntity)
                .name("Savings From Transfers")
                .savingsPlanTenor(planTenorEntity)
                .creator(appUserEntity)
                .goalId(savingsGoalEntityDao.generateSavingGoalId())
                .savingsAmount(BigDecimal.ZERO)
                .goalCategory(goalCategoryEntity)
                .build();

        return savingsGoalEntityDao.saveRecord(savingsGoalEntity);
    }*/


    private SavingsPlanTenorEntity getSavingsPlanTenor(SavingsGoalCreationRequest goalCreationRequest) {
        SavingsPlanTenorEntity planTenor;
        if(goalCreationRequest.getDurationId() != 0) {
            planTenor = savingsPlanTenorEntityDao.findById(goalCreationRequest.getDurationId())
                    .orElseThrow(() -> new BadRequestException("Invalid savings plan duration."));
        }else {
            if(goalCreationRequest.getDurationInDays() < 10) {
                throw new BusinessLogicConflictException("Savings duration must be a minimum of 10 days.");
            }
            Optional<SavingsPlanTenorEntity> durationOptional = savingsPlanTenorEntityDao.findSavingsPlanTenorForDuration(goalCreationRequest.getDurationInDays());
            if(!durationOptional.isPresent()) {
                int maximumDuration = savingsPlanTenorEntityDao.getMaximumSavingsDuration();
                if(goalCreationRequest.getDurationInDays() > maximumDuration) {
                    throw new BusinessLogicConflictException("Maximum savings duration is "+maximumDuration+" days.");
                }
                // very unlikely to happen
                throw new BusinessLogicConflictException("Sorry, selected duration is currently not supported.");
            }
            planTenor = durationOptional.get();
        }
        return planTenor;
    }

    @Transactional
    @Override
    public SavingsGoalModel createNewSavingsGoal(AuthenticatedUser currentUser, SavingsGoalCreationRequest goalCreationRequest) {

        log.info("Request payload: {}", goalCreationRequest.toString());

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());
        SavingsGoalCategoryEntity savingsGoalCategory = savingsGoalCategoryEntityDao.findCategoryByCode(goalCreationRequest.getCategoryCode())
                .orElseThrow(() -> new BadRequestException("Invalid savings goal category code."));

        SavingsPlanEntity savingsPlan;
        if(!StringUtils.isEmpty(goalCreationRequest.getPlanId())) {
            savingsPlan = savingsPlanEntityDao.findPlanByPlanId(goalCreationRequest.getPlanId()).orElseThrow(() -> new BadRequestException("Invalid savings plan Id."));
        }else {
            savingsPlan = savingsPlanEntityDao.getPlanByType(SavingsPlanTypeConstant.SAVINGS_TIER_ONE);
        }
        SavingsPlanTenorEntity planTenor = getSavingsPlanTenor(goalCreationRequest);
        int selectedDuration = planTenor.getDuration();
        boolean lockedSavings = true;
        if(goalCreationRequest.getDurationInDays() != 0) {
            lockedSavings = goalCreationRequest.isLockedSavings();
            selectedDuration = goalCreationRequest.getDurationInDays();
        }

        if(planTenor.getSavingsPlan() != null && selectedDuration == 0) {
            if(!savingsPlan.getId().equals(planTenor.getSavingsPlan().getId())){
                throw new BadRequestException("Invalid savings duration for selected plan.");
            }
        }

        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountId(goalCreationRequest.getDebitAccountId())
                .orElseThrow(() -> {
                    log.info("Bank account Id - {} not found", goalCreationRequest.getDebitAccountId());
                    return new BadRequestException("Invalid debit account Id.");
                });

        if(!mintAccount.getId().equals(debitAccount.getMintAccount().getId())) {
            throw new UnauthorisedException("Request denied.");
        }

        /*
        if(savingsGoalEntityDao.countUserCreatedAccountSavingsGoals(mintAccount) >= 5) {
            throw new BusinessLogicConflictException("Sorry, you have reached the maximum(5) active saving goals permitted for an account.");
        }
        */

        String goalName = goalCreationRequest.getName();
        if(savingsGoalEntityDao.findGoalByNameAndPlanAndAccount(goalName, savingsPlan, mintAccount).isPresent()) {
            throw new BadRequestException("You already have a savings goal with same name.");
        }
        BigDecimal targetAmount = BigDecimal.valueOf(goalCreationRequest.getTargetAmount());
        BigDecimal fundingAmount = BigDecimal.valueOf(goalCreationRequest.getFundingAmount());
        validateTier(debitAccount, savingsPlan);

        LocalDateTime maturityDate = LocalDateTime.now().plusDays(selectedDuration);
        LocalDateTime nextSavingsDate = null;
        SavingsFrequencyTypeConstant frequencyType = SavingsFrequencyTypeConstant.NONE;
        SavingsGoalStatusConstant statusConstant = SavingsGoalStatusConstant.ACTIVE;
        if(goalCreationRequest.isAutoDebit()) {
            if(goalCreationRequest.getStartDate() == null || StringUtils.isEmpty(goalCreationRequest.getFrequency())) {
                throw new BadRequestException("Start date and frequency is required.");
            }
            LocalDateTime startDate = goalCreationRequest.getStartDate().atTime(LocalTime.now());
            frequencyType = SavingsFrequencyTypeConstant.valueOf(goalCreationRequest.getFrequency());
            nextSavingsDate = startDate.plusHours(1).withMinute(0).withSecond(0);
            statusConstant = SavingsGoalStatusConstant.ACTIVE;
        }else {
            validateAmount(debitAccount, targetAmount, fundingAmount, savingsPlan);
        }

        SavingsGoalEntity savingsGoalEntity = SavingsGoalEntity.builder()
                .mintAccount(mintAccount)
                .creator(appUser)
                .goalCategory(savingsGoalCategory)
                .savingsGoalType(SavingsGoalTypeConstant.CUSTOMER_SAVINGS)
                .goalStatus(statusConstant)
                .goalId(savingsGoalEntityDao.generateSavingGoalId())
                .savingsAmount(fundingAmount)
                .targetAmount(targetAmount)
                .accruedInterest(BigDecimal.ZERO)
                .savingsBalance(BigDecimal.ZERO)
                .savingsFrequency(frequencyType)
                .autoSave(goalCreationRequest.isAutoDebit())
                .savingsPlan(savingsPlan)
                .savingsPlanTenor(planTenor)
                .interestRate(planTenor.getInterestRate())
                .creationSource(SavingsGoalCreationSourceConstant.CUSTOMER)
                .name(goalName)
                .maturityDate(maturityDate)
                .lockedSavings(lockedSavings)
                .selectedDuration(selectedDuration)
                .nextAutoSaveDate(nextSavingsDate)
                .savingsStartDate(goalCreationRequest.getStartDate())
                .build();

        savingsGoalEntity = savingsGoalEntityDao.saveRecord(savingsGoalEntity);
        log.info("Savings goal {} created with id: {}", goalName, savingsGoalEntity.getGoalId());

        if(!goalCreationRequest.isAutoDebit()) {
            SavingsGoalFundingResponse fundingResponse = fundSavingsGoalUseCase.fundSavingGoal(debitAccount, appUser,  savingsGoalEntity, fundingAmount);
            if(!fundingResponse.getResponseCode().equalsIgnoreCase("00")) {
                throw new BusinessLogicConflictException("Sorry, temporary unable to fund your saving goal. Please try again later.");
            }
        }
       /* SavingsGoalCreationEvent goalCreationEvent = SavingsGoalCreationEvent.builder()
                .goalId(savingsGoalEntity.getGoalId())
                .accountId(mintAccount.getAccountId())
                .savingsBalance(savingsGoalEntity.getSavingsBalance())
                .name(goalName)
                .withdrawalAccountNumber(debitAccount.getAccountNumber())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.SAVING_GOAL_CREATION, new EventModel<>(goalCreationEvent)); */
        SavingsGoalModel goalModel = getSavingsGoalUseCase.fromSavingsGoalEntityToModel(savingsGoalEntity);
        return goalModel;


    }

    private void validateAmount(MintBankAccountEntity debitAccount, BigDecimal targetAmount, BigDecimal fundAmount, SavingsPlanEntity savingsPlanEntity) {
        if(debitAccount.getAvailableBalance().compareTo(fundAmount) < 0) {
            throw new BusinessLogicConflictException("You have insufficient balance for fund your savings goal.");
        }
        if(fundAmount.compareTo(targetAmount) > 0) {
            throw new BadRequestException("Amount to be funded is already greater than target amount. Please increase target amount.");
        }
        /*if(targetAmount.compareTo(savingsPlanEntity.getMaximumBalance()) > 0 && savingsPlanEntity.getMaximumBalance().doubleValue() > 0) {
            throw new BadRequestException("Target amount cannot be greater than the savings plan maximum balance.");
        }
        if(fundAmount.compareTo(savingsPlanEntity.getMinimumBalance()) < 0) {
            throw new BadRequestException("Amount to fund cannot be less than the savings plan minimum balance.");
        }*/

    }

    private void validateTier(MintBankAccountEntity bankAccountEntity, SavingsPlanEntity planEntity) {
        SavingsPlanTypeConstant planTypeConstant = planEntity.getPlanName();
        TierLevelEntity tierLevelEntity =  tierLevelEntityDao.getRecordById(bankAccountEntity.getAccountTierLevel().getId());
        if(tierLevelEntity.getLevel() == TierLevelTypeConstant.TIER_ONE) {
            if(planTypeConstant != SavingsPlanTypeConstant.SAVINGS_TIER_ONE) {
                throw new BusinessLogicConflictException("Sorry, you can only use "+SavingsPlanTypeConstant.SAVINGS_TIER_ONE.getName()+" " +
                        "until your account is verified and upgraded.");
            }
            // maximum of 3 tier one goals expected.
            long totalTierOneSavingGoals = savingsGoalEntityDao.countUserCreatedSavingsGoalsOnPlan(bankAccountEntity.getMintAccount(), planEntity);
            if(totalTierOneSavingGoals >= 3) {
                throw new BusinessLogicConflictException("Sorry, maximum of 3 saving goals allowed for your account until it is verified.");
            }
        }
    }


    @Async
    @Override
    public void runInterestUpdate() {
        int size = 1000;
        int page = 0;
        PagedResponse<SavingsGoalEntity> pagedResponse = savingsGoalEntityDao.getPagedEligibleInterestSavingsGoal(page, size);
        processInterestUpdate(pagedResponse.getRecords());
        for(page = 1; page < pagedResponse.getTotalPages(); page++) {
            pagedResponse = savingsGoalEntityDao.getPagedEligibleInterestSavingsGoal(page, size);
            processInterestUpdate(pagedResponse.getRecords());
        }
    }
    private void processInterestUpdate(List<SavingsGoalEntity> goalEntityList) {
        for(SavingsGoalEntity savingsGoalEntity : goalEntityList) {
            if(savingsGoalEntity.getInterestRate() != 0.0) {
                continue;
            }
            Optional<SavingsInterestEntity> firstInterestOpt = savingsInterestEntityDao.findFirstInterestApplied(savingsGoalEntity);
            if(!firstInterestOpt.isPresent()) {
                continue;
            }
            SavingsInterestEntity interestEntity = firstInterestOpt.get();
            savingsGoalEntity.setInterestRate(interestEntity.getRate());
            savingsGoalEntityDao.saveRecord(savingsGoalEntity);
        }
    }
}
