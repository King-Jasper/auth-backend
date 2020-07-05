package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.UpdateSavingsGoalMaturityUseCase;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
@Slf4j
@Named
public class SavingsMaturityUpdateJob {

    private final UpdateSavingsGoalMaturityUseCase updateSavingsGoalMaturityUseCase;
    public SavingsMaturityUpdateJob(UpdateSavingsGoalMaturityUseCase updateSavingsGoalMaturityUseCase) {
        this.updateSavingsGoalMaturityUseCase = updateSavingsGoalMaturityUseCase;
    }

    @Scheduled(cron = "0 0 0/2 ? * *") // runs every 2 hours every day
    @SchedulerLock(name = "SavingsMaturityUpdateJob_processSavingsMaturityUpdate", lockAtMostForString = "PT30M")
    public void processSavingsMaturityUpdate() {
        log.info("cron task processSavingsMaturityUpdate");
        updateSavingsGoalMaturityUseCase.updateStatusForMaturedSavingsGoal();
    }
}
