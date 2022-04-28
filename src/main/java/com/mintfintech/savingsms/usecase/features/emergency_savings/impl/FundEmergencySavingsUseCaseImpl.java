package com.mintfintech.savingsms.usecase.features.emergency_savings.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsPlanEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.PublishTransactionNotificationUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalFundingFailureEvent;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import com.mintfintech.savingsms.usecase.features.emergency_savings.FundEmergencySavingsUseCase;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@FieldDefaults(makeFinal = true)
@Slf4j
@Named
@AllArgsConstructor
public class FundEmergencySavingsUseCaseImpl implements FundEmergencySavingsUseCase {

    private SavingsPlanEntityDao savingsPlanEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private ApplicationEventService applicationEventService;
    private UpdateBankAccountBalanceUseCase updateAccountBalanceUseCase;
    private AppUserEntityDao appUserEntityDao;
    private SystemIssueLogService systemIssueLogService;
    private PublishTransactionNotificationUseCase publishTransactionNotificationUseCase;
    private FundSavingsGoalUseCase fundSavingsGoalUseCase;

    @Override
    public void processSavingsGoalScheduledSavingV2() {
        LocalDateTime now = LocalDateTime.now();
        List<SavingsGoalEntity> savingsGoalEntityList = savingsGoalEntityDao.getSavingGoalWithAutoSaveTime(now);
        for(SavingsGoalEntity savingsGoalEntity: savingsGoalEntityList) {
            if(savingsGoalEntity.getGoalStatus() != SavingsGoalStatusConstant.ACTIVE || !savingsGoalEntity.isAutoSave()) {
                log.info("Savings goal auto funding skipped: {}", savingsGoalEntity.getGoalId());
                continue;
            }
            LocalDateTime adjustedTime = now.withNano(0).withSecond(0).withMinute(0);
            LocalDateTime nextSavingsDate = savingsGoalEntity.getNextAutoSaveDate().withNano(0).withSecond(0).withMinute(0);
            if(!adjustedTime.equals(nextSavingsDate)) {
                log.info("Next saving date does not match:{} - {} - {}", savingsGoalEntity.getGoalId(), adjustedTime, nextSavingsDate);
                continue;
            }

            LocalDateTime newNextSavingsDate = getNewNextSavingsDate(savingsGoalEntity, nextSavingsDate);
            MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(savingsGoalEntity.getMintAccount(), BankAccountTypeConstant.CURRENT);
            debitAccount = updateAccountBalanceUseCase.processBalanceUpdate(debitAccount);
            BigDecimal savingsAmount = savingsGoalEntity.getSavingsAmount();

            if(debitAccount.getAvailableBalance().compareTo(savingsAmount) < 0) {
                log.info("Insufficient balance for savings auto debit: {}" , savingsGoalEntity.getGoalId());
                savingsGoalEntity.setNextAutoSaveDate(newNextSavingsDate);
                savingsGoalEntityDao.saveRecord(savingsGoalEntity);
                publishTransactionNotificationUseCase.sendSavingsFundingFailureNotification(savingsGoalEntity, savingsAmount, "Insufficient balance to fund savings goal.");
                continue;
            }
            savingsGoalEntity.setNextAutoSaveDate(newNextSavingsDate);
            savingsGoalEntityDao.saveRecord(savingsGoalEntity);
            boolean proceedWithFunding = validateSavingTierRestriction(savingsGoalEntity, savingsAmount);
            if(!proceedWithFunding) {
                continue;
            }
            SavingsGoalFundingResponse fundingResponse = fundSavingsGoalUseCase.fundSavingGoal(debitAccount, null, savingsGoalEntity, savingsAmount);
            if(!"00".equalsIgnoreCase(fundingResponse.getResponseCode())) {
                publishTransactionNotificationUseCase.sendSavingsFundingFailureNotification(savingsGoalEntity, savingsAmount, fundingResponse.getResponseMessage());
                String message = String.format("Goal Id: %s; message: %s", savingsGoalEntity.getGoalId(),  fundingResponse.getResponseMessage());
                systemIssueLogService.logIssue("Savings Auto-Funding Failed", "Savings funding failure", message);
            }
        }
    }

    private LocalDateTime getNewNextSavingsDate(SavingsGoalEntity savingsGoalEntity, LocalDateTime nextSavingsDate) {
        LocalDateTime newNextSavingsDate = nextSavingsDate.plusDays(1);
        SavingsFrequencyTypeConstant frequencyType = savingsGoalEntity.getSavingsFrequency();
        if (frequencyType == SavingsFrequencyTypeConstant.WEEKLY) {
            newNextSavingsDate = nextSavingsDate.plusWeeks(1);
        } else if (frequencyType == SavingsFrequencyTypeConstant.MONTHLY) {
            newNextSavingsDate = nextSavingsDate.plusMonths(1);
        }
        return newNextSavingsDate;
    }

    private boolean validateSavingTierRestriction(SavingsGoalEntity goalEntity, BigDecimal savingsAmount) {
        SavingsPlanEntity savingsPlanEntity = savingsPlanEntityDao.getRecordById(goalEntity.getSavingsPlan().getId());
        if(savingsPlanEntity.getPlanName() != SavingsPlanTypeConstant.SAVINGS_TIER_THREE) {
            BigDecimal toBeNewBalance = goalEntity.getSavingsBalance().add(savingsAmount);
            if(toBeNewBalance.compareTo(savingsPlanEntity.getMaximumBalance()) > 0) {
                MintBankAccountEntity bankAccountEntity = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(goalEntity.getMintAccount(), BankAccountTypeConstant.CURRENT);
                if(bankAccountEntity.getAccountTierLevel().getLevel() != TierLevelTypeConstant.TIER_THREE) {
                    log.info("Savings goal: {} Auto debit ignored. New balance {} will be above maximum balance for plan: {}" , goalEntity.getGoalId(), toBeNewBalance, savingsPlanEntity.getMaximumBalance());
                    AppUserEntity appUserEntity = appUserEntityDao.getRecordById(goalEntity.getCreator().getId());
                    SavingsGoalFundingFailureEvent failureEvent = SavingsGoalFundingFailureEvent.builder()
                            .failureMessage("Maximum balance for savings plan is exceeded. Update savings plan.")
                            .amount(savingsAmount)
                            .status("ABORTED")
                            .name(appUserEntity.getName())
                            .recipient(appUserEntity.getEmail()).build();
                    applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_SAVINGS_GOAL_FUNDING_FAILURE, new EventModel<>(failureEvent));
                    return false;
                }
            }
        }
        return true;
    }
}
