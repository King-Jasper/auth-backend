package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsPlanTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TierLevelTypeConstant;
import com.mintfintech.savingsms.domain.services.AuditTrailService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.ChangeSavingsPlanUseCase;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@Named
@AllArgsConstructor
public class ChangeSavingsPlanUseCaseImpl implements ChangeSavingsPlanUseCase {

    private SavingsPlanEntityDao savingsPlanEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private SavingsPlanChangeEntityDao savingsPlanChangeEntityDao;
    private TierLevelEntityDao tierLevelEntityDao;
    private GetSavingsGoalUseCase getSavingsGoalUseCase;

    @Override
    public SavingsGoalModel changePlan(AuthenticatedUser authenticatedUser, String goalId, String planId) {
        MintAccountEntity mintAccountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        Optional<SavingsGoalEntity> savingsGoalEntityOptional = savingsGoalEntityDao.findSavingGoalByAccountAndGoalId(mintAccountEntity, goalId);
        if(!savingsGoalEntityOptional.isPresent()) {
            throw new BadRequestException("Invalid saving goal Id.");
        }
        SavingsGoalEntity savingsGoalEntity = savingsGoalEntityOptional.get();
        SavingsPlanEntity planEntity = savingsPlanEntityDao.findPlanByPlanId(planId).orElseThrow(() -> new BadRequestException("Invalid savings plan Id."));
        if(planEntity.getId().equals(savingsGoalEntity.getSavingsPlan().getId())) {
            throw new BusinessLogicConflictException("You are already on "+planEntity.getPlanName().getName()+" plan");
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
        SavingsPlanChangeEntity planChangeEntity = SavingsPlanChangeEntity.builder()
                .currentPlan(savingsGoalEntity.getSavingsPlan())
                .newPlan(planEntity)
                .savingsGoal(savingsGoalEntity)
                .accruedInterest(savingsGoalEntity.getAccruedInterest())
                .savingAmount(savingsGoalEntity.getSavingsAmount())
                .build();
        savingsPlanChangeEntityDao.saveRecord(planChangeEntity);
        savingsGoalEntity.setSavingsPlan(planEntity);
        savingsGoalEntityDao.saveRecord(savingsGoalEntity);

        return getSavingsGoalUseCase.fromSavingsGoalEntityToModel(savingsGoalEntity);
    }
}
