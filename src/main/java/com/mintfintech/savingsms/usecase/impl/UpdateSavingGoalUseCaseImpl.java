package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsFrequencyTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TierLevelTypeConstant;
import com.mintfintech.savingsms.domain.services.AuditTrailService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.UpdateSavingGoalUseCase;
import com.mintfintech.savingsms.usecase.data.request.SavingsFrequencyUpdateRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Named
@AllArgsConstructor
public class UpdateSavingGoalUseCaseImpl implements UpdateSavingGoalUseCase {

    private MintAccountEntityDao mintAccountEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private TierLevelEntityDao tierLevelEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private SavingsPlanEntityDao savingsPlanEntityDao;
    private GetSavingsGoalUseCase getSavingsGoalUseCase;
    private AuditTrailService auditTrailService;

    @Override
    public SavingsGoalModel updateSavingFrequency(AuthenticatedUser currentUser, SavingsFrequencyUpdateRequest autoSaveRequest) {

        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());
        SavingsGoalEntity savingsGoal = savingsGoalEntityDao.findSavingGoalByAccountAndGoalId(accountEntity, autoSaveRequest.getGoalId())
                .orElseThrow(() -> new BadRequestException("Invalid savings goal Id."));
        SavingsPlanEntity planEntity = savingsPlanEntityDao.getRecordById(savingsGoal.getSavingsPlan().getId());

        BigDecimal savingsAmount = BigDecimal.valueOf(autoSaveRequest.getAmount());
        if(planEntity.getMaximumBalance().compareTo(BigDecimal.ZERO) > 0 && savingsAmount.compareTo(planEntity.getMaximumBalance()) > 0) {
            throw new BusinessLogicConflictException("The maximum amount for your savings plan is N"+ MoneyFormatterUtil.priceWithDecimal(planEntity.getMaximumBalance()));
        }

        MintBankAccountEntity currentAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(accountEntity, BankAccountTypeConstant.CURRENT);
        TierLevelEntity tierLevelEntity = tierLevelEntityDao.getRecordById(currentAccount.getAccountTierLevel().getId());
        if(tierLevelEntity.getLevel() != TierLevelTypeConstant.TIER_THREE) {
            if(savingsAmount.compareTo(tierLevelEntity.getBulletTransactionAmount()) > 0) {
                throw new BadRequestException("Sorry, transaction limit on your account tier is N"+MoneyFormatterUtil.priceWithDecimal(tierLevelEntity.getBulletTransactionAmount()));
            }
        }

        SavingsGoalEntity oldState = new SavingsGoalEntity();
        BeanUtils.copyProperties(savingsGoal, oldState);

        SavingsFrequencyTypeConstant frequencyTypeConstant = SavingsFrequencyTypeConstant.valueOf(autoSaveRequest.getFrequency());
        LocalDateTime nextSavingsDate;
        LocalDateTime nearestHour = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0);
        if(frequencyTypeConstant == SavingsFrequencyTypeConstant.DAILY) {
            nextSavingsDate = nearestHour.plusDays(1);
        }else if(frequencyTypeConstant == SavingsFrequencyTypeConstant.WEEKLY) {
            nextSavingsDate = nearestHour.plusWeeks(1);
        }else {
            nextSavingsDate = nearestHour.plusMonths(1);
        }
        savingsGoal.setAutoSave(true);
        savingsGoal.setNextAutoSaveDate(nextSavingsDate);
        savingsGoal.setSavingsAmount(savingsAmount);
        savingsGoal.setSavingsFrequency(frequencyTypeConstant);
        savingsGoal = savingsGoalEntityDao.saveRecord(savingsGoal);

        String description = String.format("Savings frequency: %s set on goal: %s", savingsGoal.getSavingsFrequency().name(), savingsGoal.getName());
        auditTrailService.createAuditLog(AuditTrailService.AuditType.UPDATE, description, savingsGoal, oldState);
        return getSavingsGoalUseCase.fromSavingsGoalEntityToModel(savingsGoal);
    }

    @Override
    public void cancelSavingFrequency(AuthenticatedUser currentUser, String goalId) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());
        SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.findSavingGoalByAccountAndGoalId(accountEntity, goalId)
                .orElseThrow(() -> new BadRequestException("Invalid savings goal Id."));

        SavingsGoalEntity oldState = new SavingsGoalEntity();
        BeanUtils.copyProperties(savingsGoalEntity, oldState);

        savingsGoalEntity.setAutoSave(false);
        savingsGoalEntity.setSavingsFrequency(SavingsFrequencyTypeConstant.NONE);
        savingsGoalEntity = savingsGoalEntityDao.saveRecord(savingsGoalEntity);

        String description = String.format("Cancelled saving frequency: %s on goal %s", oldState.getSavingsFrequency().name(), savingsGoalEntity.getName());
        auditTrailService.createAuditLog(AuditTrailService.AuditType.UPDATE, description, savingsGoalEntity, oldState);
    }

}
