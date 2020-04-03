package com.mintfintech.savingsms.usecase.impl;

import com.google.gson.Gson;
import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.AuditTrailService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.CreateSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalCreationEvent;
import com.mintfintech.savingsms.usecase.data.request.SavingsGoalCreationRequest;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import javax.inject.Named;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
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
    private TierLevelEntityDao tierLevelEntityDao;
    private GetSavingsGoalUseCase getSavingsGoalUseCase;
    private FundSavingsGoalUseCase fundSavingsGoalUseCase;
    private ApplicationEventService applicationEventService;
    private AuditTrailService auditTrailService;

    @Override
    public SavingsGoalEntity createDefaultSavingsGoal(MintAccountEntity mintAccountEntity, AppUserEntity appUserEntity) {
        SavingsGoalCategoryEntity goalCategoryEntity = savingsGoalCategoryEntityDao.findCategoryByCode("08").get();
        SavingsPlanEntity savingsPlanEntity = savingsPlanEntityDao.getPlanByType(SavingsPlanTypeConstant.SAVINGS_TIER_ONE);
        SavingsPlanTenorEntity planTenorEntity = savingsPlanTenorEntityDao.getSavingPlanTenor(savingsPlanEntity, 30, SavingsDurationTypeConstant.DAYS);
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
                .name("Mint Accrued Interest.")
                .savingsPlanTenor(planTenorEntity)
                .creator(appUserEntity)
                .goalId(savingsGoalEntityDao.generateSavingGoalId())
                .savingsAmount(BigDecimal.ZERO)
                .goalCategory(goalCategoryEntity)
                .build();
        return savingsGoalEntityDao.saveRecord(savingsGoalEntity);
    }

    @Transactional
    @Override
    public SavingsGoalModel createNewSavingsGoal(AuthenticatedUser currentUser, SavingsGoalCreationRequest goalCreationRequest) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());
        SavingsGoalCategoryEntity savingsGoalCategory = savingsGoalCategoryEntityDao.findCategoryByCode(goalCreationRequest.getCategoryCode())
                .orElseThrow(() -> new BadRequestException("Invalid savings goal category code."));

        SavingsPlanEntity savingsPlan = savingsPlanEntityDao.findPlanByPlanId(goalCreationRequest.getPlanId())
                .orElseThrow(() -> new BadRequestException("Invalid savings plan Id."));

        SavingsPlanTenorEntity planTenor = savingsPlanTenorEntityDao.findById(goalCreationRequest.getDurationId())
                .orElseThrow(() -> new BadRequestException("Invalid savings plan duration."));

        if(!savingsPlan.getId().equals(planTenor.getSavingsPlan().getId())){
            throw new BadRequestException("Invalid savings duration for selected plan.");
        }
        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountId(goalCreationRequest.getDebitAccountId())
                .orElseThrow(() -> new BadRequestException("Invalid debit account Id."));
        if(!mintAccount.getId().equals(debitAccount.getMintAccount().getId())) {
            throw new UnauthorisedException("Request denied.");
        }

        String goalName = goalCreationRequest.getName();
        if(savingsGoalEntityDao.findGoalByNameAndPlanAndAccount(goalName, savingsPlan, mintAccount).isPresent()) {
            throw new BadRequestException("You already have a savings goal with same name.");
        }
        BigDecimal targetAmount = BigDecimal.valueOf(goalCreationRequest.getTargetAmount());
        BigDecimal fundingAmount = BigDecimal.valueOf(goalCreationRequest.getFundingAmount());
        validateAmount(debitAccount, targetAmount, fundingAmount, savingsPlan);
        validateTier(debitAccount, savingsPlan);

        LocalDateTime maturityDate = LocalDateTime.now().plusDays(planTenor.getDuration());

        SavingsGoalEntity savingsGoalEntity = SavingsGoalEntity.builder()
                .mintAccount(mintAccount)
                .creator(appUser)
                .goalCategory(savingsGoalCategory)
                .savingsGoalType(SavingsGoalTypeConstant.CUSTOMER_SAVINGS)
                .goalStatus(SavingsGoalStatusConstant.ACTIVE)
                .goalId(savingsGoalEntityDao.generateSavingGoalId())
                .savingsAmount(fundingAmount)
                .targetAmount(targetAmount)
                .accruedInterest(BigDecimal.ZERO)
                .savingsBalance(BigDecimal.ZERO)
                .savingsFrequency(SavingsFrequencyTypeConstant.NONE)
                .autoSave(false)
                .savingsPlan(savingsPlan)
                .savingsPlanTenor(planTenor)
                .creationSource(SavingsGoalCreationSourceConstant.CUSTOMER)
                .name(goalName)
                .maturityDate(maturityDate)
                .build();

        savingsGoalEntity = savingsGoalEntityDao.saveRecord(savingsGoalEntity);
        log.info("Savings goal {} created with id: {}", goalName, savingsGoalEntity.getGoalId());

       /* try {
            SavingsGoalEntity oldState = new SavingsGoalEntity();
            BeanUtils.copyProperties(savingsGoalEntity, oldState);
            auditTrailService.createAuditLog(AuditTrailService.AuditType.CREATE, "test", oldState);
        }catch (Exception ex){ex.printStackTrace();} */

        SavingsGoalFundingResponse fundingResponse = fundSavingsGoalUseCase.fundSavingGoal(debitAccount, appUser,  savingsGoalEntity, fundingAmount);
        if(!fundingResponse.getResponseCode().equalsIgnoreCase("00")) {
            throw new BusinessLogicConflictException("Sorry, temporary unable to fund your saving goal. Please try again later.");
        }


        SavingsGoalCreationEvent goalCreationEvent = SavingsGoalCreationEvent.builder()
                .goalId(savingsGoalEntity.getGoalId())
                .accountId(mintAccount.getAccountId())
                .savingsBalance(savingsGoalEntity.getSavingsBalance())
                .name(goalName)
                .withdrawalAccountNumber(debitAccount.getAccountNumber())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.SAVING_GOAL_CREATION, new EventModel<>(goalCreationEvent));

        return getSavingsGoalUseCase.fromSavingsGoalEntityToModel(savingsGoalEntity);
    }

    private void  validateAmount(MintBankAccountEntity debitAccount, BigDecimal targetAmount, BigDecimal fundAmount, SavingsPlanEntity savingsPlanEntity) {
        if(debitAccount.getAvailableBalance().compareTo(fundAmount) < 0) {
            throw new BusinessLogicConflictException("You have insufficient balance for fund your savings goal.");
        }

        if(fundAmount.compareTo(targetAmount) > 0) {
            throw new BadRequestException("Amount to be funded cannot be greater than the saving target amount.");
        }
        if(targetAmount.compareTo(savingsPlanEntity.getMaximumBalance()) > 0) {
            throw new BadRequestException("Target amount cannot be greater than the saving plan maximum balance.");
        }
        if(fundAmount.compareTo(savingsPlanEntity.getMinimumBalance()) < 0) {
            throw new BadRequestException("Amount to fund cannot be less than the saving plan minimum balance.");
        }

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
            long totalTierOneSavingGoals = savingsGoalEntityDao.countAccountSavingsGoalOnPlan(bankAccountEntity.getMintAccount(), planEntity);
            if(totalTierOneSavingGoals >= 3) {
                throw new BusinessLogicConflictException("Sorry, maximum of 3 saving goals allowed for your account until it is verified.");
            }
        }
    }
}
