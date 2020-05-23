package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.models.PagedResponse;
import com.mintfintech.savingsms.usecase.UpdateSavingsGoalMaturityUseCase;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
@Slf4j
@Named
public class UpdateSavingsGoalMaturityUseCaseImpl implements UpdateSavingsGoalMaturityUseCase {

    private SavingsGoalEntityDao savingsGoalEntityDao;
    public UpdateSavingsGoalMaturityUseCaseImpl(SavingsGoalEntityDao savingsGoalEntityDao) {
        this.savingsGoalEntityDao = savingsGoalEntityDao;
    }

    @Override
    public void updateStatusForMaturedSavingsGoal() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime, endTime;
        if(now.getHour() > 12)  {
            startTime = now.withHour(12).withMinute(0);
            endTime = LocalDateTime.of(LocalDate.now(), LocalTime.MAX).withNano(0);
        }else {
            startTime = LocalDate.now().atStartOfDay();
            endTime = now.withHour(12).withMinute(0);
        }

        int size = 50;
        PagedResponse<SavingsGoalEntity> pagedResponse = savingsGoalEntityDao.getPagedSavingsGoalsWithMaturityDateWithinPeriod(startTime, endTime, 0, size);
        processMaturityStatusUpdate(pagedResponse.getRecords());
        int totalPages = pagedResponse.getTotalPages();
        if(pagedResponse.getTotalRecords() > 0) {
            log.info("Savings goal for interest consideration: {}", pagedResponse.getTotalRecords());
        }
        for(int i = 1; i < totalPages; i++) {
            pagedResponse = savingsGoalEntityDao.getPagedEligibleInterestSavingsGoal(i, size);
            processMaturityStatusUpdate(pagedResponse.getRecords());
        }
    }

    private void processMaturityStatusUpdate(List<SavingsGoalEntity> savingsGoalEntityList) {
        for(SavingsGoalEntity savingsGoalEntity: savingsGoalEntityList) {
             long remainingDays = LocalDateTime.now().until(savingsGoalEntity.getMaturityDate(), ChronoUnit.DAYS);
             if(remainingDays > 0) {
                 log.info("Goal :{} not yet matured: {}", savingsGoalEntity.getGoalId(), savingsGoalEntity.getMaturityDate().toString());
                 continue;
             }
             if(savingsGoalEntity.getGoalStatus() != SavingsGoalStatusConstant.ACTIVE) {
                 log.info("Goal :{} is not active", savingsGoalEntity.getGoalId());
                  continue;
             }
             savingsGoalEntity.setGoalStatus(SavingsGoalStatusConstant.MATURED);
             savingsGoalEntityDao.saveRecord(savingsGoalEntity);
        }
    }
}
