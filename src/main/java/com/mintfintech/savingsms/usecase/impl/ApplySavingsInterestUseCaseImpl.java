package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsInterestEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsPlanEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsInterestEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.PagedResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.usecase.ApplySavingsInterestUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalBalanceUpdateEvent;
import com.mintfintech.savingsms.utils.DateUtil;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@FieldDefaults(makeFinal = true)
@Slf4j
@Named
@AllArgsConstructor
public class ApplySavingsInterestUseCaseImpl implements ApplySavingsInterestUseCase {

    private SavingsGoalEntityDao savingsGoalEntityDao;
    private ApplicationEventService applicationEventService;
    private SavingsInterestEntityDao savingsInterestEntityDao;
    private SavingsPlanEntityDao savingsPlanEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;

    @Override
    public void processInterestAndUpdateGoals() {
        int size = 50;
        PagedResponse<SavingsGoalEntity> pagedResponse = savingsGoalEntityDao.getPagedEligibleInterestSavingsGoal(0, size);
        processInterestComputation(pagedResponse.getRecords());
        int totalPages = pagedResponse.getTotalPages();
        if(pagedResponse.getTotalRecords() > 0) {
            log.info("Savings goal for interest consideration: {}", pagedResponse.getTotalRecords());
        }
        for(int i = 1; i < totalPages; i++) {
            pagedResponse = savingsGoalEntityDao.getPagedEligibleInterestSavingsGoal(i, size);
            processInterestComputation(pagedResponse.getRecords());
        }
    }
    private void processInterestComputation(List<SavingsGoalEntity> savingsGoalEntityList) {
        for(SavingsGoalEntity savingsGoalEntity: savingsGoalEntityList) {
             if(!shouldApplyInterest(savingsGoalEntity)) {
                 log.info("Interest not applied to goal {}: {}", savingsGoalEntity.getGoalId(), savingsGoalEntity.getName());
                 continue;
             }
             applyInterest(savingsGoalEntity);
             publishInterestApplication(savingsGoalEntity);
        }
    }

    private void applyInterest(SavingsGoalEntity savingsGoalEntity) {
        SavingsPlanEntity planEntity = savingsPlanEntityDao.getRecordById(savingsGoalEntity.getSavingsPlan().getId());
        BigDecimal interestRatePerDay = BigDecimal.valueOf(planEntity.getInterestRate() / (100.0 * 365.0));
        BigDecimal interest = savingsGoalEntity.getSavingsBalance().multiply(interestRatePerDay).setScale(2, BigDecimal.ROUND_CEILING);

        SavingsInterestEntity savingsInterestEntity = SavingsInterestEntity.builder()
                .interest(interest)
                .savingsBalance(savingsGoalEntity.getSavingsBalance())
                .savingsGoal(savingsGoalEntity)
                .rate(planEntity.getInterestRate())
                .build();
        savingsInterestEntityDao.saveRecord(savingsInterestEntity);
        savingsGoalEntity.setAccruedInterest(savingsInterestEntityDao.getTotalInterestAmountOnGoal(savingsGoalEntity));
        savingsGoalEntity.setLastInterestApplicationDate(LocalDateTime.now());
        savingsGoalEntityDao.saveRecord(savingsGoalEntity);
    }

    private boolean shouldApplyInterest(SavingsGoalEntity savingsGoalEntity) {
       /* if(savingsGoalEntity.getCreationSource() != SavingsGoalCreationSourceConstant.CUSTOMER) {
            log.info("Saving goal not created by customer.");
            return false;
        }*/
        if(savingsGoalEntity.getCreationSource() == SavingsGoalCreationSourceConstant.MINT && savingsGoalEntity.getSavingsBalance().compareTo(BigDecimal.valueOf(50000.0)) >= 0){
            log.info("Interest cannot be applied to mint savings goal above 50k.");
            return false;
        }
        if(savingsGoalEntity.getGoalStatus() != SavingsGoalStatusConstant.ACTIVE) {
            log.info("Saving goal is not longer active.");
            return false;
        }
        if(savingsGoalEntity.getLastInterestApplicationDate() != null) {
            boolean interestAppliedToday = DateUtil.sameDay(LocalDateTime.now(), savingsGoalEntity.getLastInterestApplicationDate());
            log.info("Interest has been applied today: {}", interestAppliedToday);
            return !interestAppliedToday;
        }else {
            if(savingsInterestEntityDao.countInterestOnGoal(savingsGoalEntity) == 0) {
                return true;
            }
            log.info("This is weird. Savings interest exist but LastInterestApplicationDate is null. Hmmmmm.. Skipped.");
        }
        return false;
    }

    private void publishInterestApplication(SavingsGoalEntity savingsGoalEntity) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getRecordById(savingsGoalEntity.getMintAccount().getId());
        SavingsGoalBalanceUpdateEvent updateEvent = SavingsGoalBalanceUpdateEvent.builder()
                .goalId(savingsGoalEntity.getGoalId())
                .accountId(accountEntity.getAccountId())
                .accruedInterest(savingsGoalEntity.getAccruedInterest())
                .savingsBalance(savingsGoalEntity.getSavingsBalance())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.SAVING_GOAL_BALANCE_UPDATE, new EventModel<>(updateEvent));
    }
}
