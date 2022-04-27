package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
@Named
public class SavingsFundingJob {

    private final FundSavingsGoalUseCase fundSavingsGoalUseCase;
    public SavingsFundingJob(FundSavingsGoalUseCase fundSavingsGoalUseCase) {
        this.fundSavingsGoalUseCase = fundSavingsGoalUseCase;
    }

    @SchedulerLock(name = "SavingsFundingJob_processSavingFundingForSetInterval", lockAtMostForString = "PT30M")
    @Scheduled(cron = "0 0 0/1 1/1 * ?") // runs every one hour.
    public void processSavingFundingForSetInterval() {
        fundSavingsGoalUseCase.processSavingsGoalScheduledSaving();
    }

    @SchedulerLock(name = "SavingsFundingJob_processSavingFundingForSetIntervalV2", lockAtMostForString = "PT30M")
    @Scheduled(cron = "0 0 0/1 1/1 * ?") // runs every one hour.
    public void processSavingFundingForSetIntervalV2(){fundSavingsGoalUseCase.processSavingsGoalScheduledSavingV2();}
}
