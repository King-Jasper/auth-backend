package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.UpdateSavingsGoalMaturityUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
@Slf4j
@Named
public class SavingsMaturityUpdateJob {

    private UpdateSavingsGoalMaturityUseCase updateSavingsGoalMaturityUseCase;
    public SavingsMaturityUpdateJob(UpdateSavingsGoalMaturityUseCase updateSavingsGoalMaturityUseCase) {
        this.updateSavingsGoalMaturityUseCase = updateSavingsGoalMaturityUseCase;
    }

    @Scheduled(cron = "0 0 9,12,15 ? * *") // runs by 9am, 12noon, 3pm every day
    public void processSavingsMaturityUpdate() {
        log.info("cron task processSavingsMaturityUpdate");
        updateSavingsGoalMaturityUseCase.updateStatusForMaturedSavingsGoal();
    }
}
