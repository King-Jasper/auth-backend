package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.ChangeSavingsPlanUseCase;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.request.PlanChangeRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.inject.Named;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@FieldDefaults(makeFinal = true)
@Named
@AllArgsConstructor
public class ChangeSavingsPlanUseCaseImpl implements ChangeSavingsPlanUseCase {

    private SavingsPlanEntityDao savingsPlanEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private SavingsPlanChangeEntityDao savingsPlanChangeEntityDao;
    private SavingsPlanTenorEntityDao savingsPlanTenorEntityDao;
    private TierLevelEntityDao tierLevelEntityDao;
    private GetSavingsGoalUseCase getSavingsGoalUseCase;

    @Override
    public SavingsGoalModel changePlan(AuthenticatedUser authenticatedUser, String goalId, PlanChangeRequest changeRequest) {
        MintAccountEntity mintAccountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        Optional<SavingsGoalEntity> savingsGoalEntityOptional = savingsGoalEntityDao.findSavingGoalByAccountAndGoalId(mintAccountEntity, goalId);
        if(!savingsGoalEntityOptional.isPresent()) {
            throw new BadRequestException("Invalid saving goal Id.");
        }
        SavingsGoalEntity savingsGoalEntity = savingsGoalEntityOptional.get();
        if(savingsGoalEntity.getCreationSource() == SavingsGoalCreationSourceConstant.MINT){
            throw new BusinessLogicConflictException("Sorry, this goal cannot be updated because it's created by the system.");
        }
        SavingsPlanEntity planEntity = savingsPlanEntityDao.findPlanByPlanId(changeRequest.getPlanId()).orElseThrow(() -> new BadRequestException("Invalid savings plan Id."));
        if(planEntity.getId().equals(savingsGoalEntity.getSavingsPlan().getId())) {
            throw new BusinessLogicConflictException("You are already on "+planEntity.getPlanName().getName()+" plan");
        }
        SavingsPlanTenorEntity planTenor = savingsPlanTenorEntityDao.findById(changeRequest.getDurationId()).orElseThrow(() -> new BadRequestException("Invalid savings tenor Id."));
        if(!planTenor.getSavingsPlan().getId().equals(planEntity.getId())) {
            throw new BadRequestException("Savings duration is not part of the selected savings plan.");
        }
        MintBankAccountEntity bankAccountEntity = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccountEntity, BankAccountTypeConstant.CURRENT);
        TierLevelEntity tierLevelEntity = tierLevelEntityDao.getRecordById(bankAccountEntity.getAccountTierLevel().getId());
        if(tierLevelEntity.getLevel() == TierLevelTypeConstant.TIER_ONE) {
            throw new BadRequestException("Sorry, you can only use "+ SavingsPlanTypeConstant.SAVINGS_TIER_ONE.getName()+" " +
                    "until your account is verified and upgraded.");
        }
        if(savingsGoalEntity.getSavingsBalance().compareTo(planEntity.getMinimumBalance()) < 0) {
            throw new BusinessLogicConflictException("Sorry, you need a minimum of N"+ MoneyFormatterUtil.priceWithDecimal(planEntity.getMinimumBalance())+"" +
                    " in your savings plan balance to upgrade.");
        }
        SavingsPlanEntity currentPlan = savingsPlanEntityDao.getRecordById(savingsGoalEntity.getSavingsPlan().getId());
        validatePlanChange(currentPlan, planEntity);

        LocalDateTime maturityDate = LocalDateTime.now().plusDays(planTenor.getDuration());

        SavingsPlanChangeEntity planChangeEntity = SavingsPlanChangeEntity.builder()
                .currentPlan(savingsGoalEntity.getSavingsPlan())
                .newPlan(planEntity)
                .savingsGoal(savingsGoalEntity)
                .accruedInterest(savingsGoalEntity.getAccruedInterest())
                .savingAmount(savingsGoalEntity.getSavingsAmount())
                .currentPlanTenor(savingsGoalEntity.getSavingsPlanTenor())
                .newPlanTenor(planTenor)
                .currentMaturityDate(savingsGoalEntity.getMaturityDate())
                .newMaturityDate(maturityDate)
                .build();
        savingsPlanChangeEntityDao.saveRecord(planChangeEntity);

        savingsGoalEntity.setSavingsPlan(planEntity);
        savingsGoalEntity.setSavingsPlanTenor(planTenor);
        savingsGoalEntity.setMaturityDate(maturityDate);
        if(savingsGoalEntity.getGoalStatus() != SavingsGoalStatusConstant.ACTIVE){
            savingsGoalEntity.setGoalStatus(SavingsGoalStatusConstant.ACTIVE);
        }
        savingsGoalEntityDao.saveRecord(savingsGoalEntity);
        return getSavingsGoalUseCase.fromSavingsGoalEntityToModel(savingsGoalEntity);
    }

    private void validatePlanChange(SavingsPlanEntity currentPlan, SavingsPlanEntity newPlan) {
        if(currentPlan.getPlanName() == SavingsPlanTypeConstant.SAVINGS_TIER_TWO) {
            if(newPlan.getPlanName() == SavingsPlanTypeConstant.SAVINGS_TIER_ONE) {
                throw new BadRequestException("Sorry, you can only upgrade from a lower plan to a higher plan or different duration.");
            }
        }else if(currentPlan.getPlanName() == SavingsPlanTypeConstant.SAVINGS_TIER_THREE){
            if(newPlan.getPlanName() != SavingsPlanTypeConstant.SAVINGS_TIER_THREE) {
                throw new BadRequestException("Sorry, you can only upgrade from a lower plan to a higher plan or different duration.");
            }
        }
    }
}
