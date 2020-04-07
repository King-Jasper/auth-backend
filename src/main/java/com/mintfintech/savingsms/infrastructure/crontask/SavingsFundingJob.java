package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
@Named
public class SavingsFundingJob {

    private FundSavingsGoalUseCase fundSavingsGoalUseCase;
    public SavingsFundingJob(FundSavingsGoalUseCase fundSavingsGoalUseCase) {
        this.fundSavingsGoalUseCase = fundSavingsGoalUseCase;
    }

    @Scheduled(cron = "0 0 0/1 1/1 * ?") // runs every one hour.
    public void processSavingFundingForSetInterval() {
        fundSavingsGoalUseCase.processSavingsGoalScheduledSaving();
    }
}
