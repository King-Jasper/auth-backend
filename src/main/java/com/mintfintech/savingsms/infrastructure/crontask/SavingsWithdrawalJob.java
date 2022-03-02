package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.FundWithdrawalUseCase;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
@Slf4j
@Named
public class SavingsWithdrawalJob {

    private final FundWithdrawalUseCase fundWithdrawalUseCase;
    public SavingsWithdrawalJob(FundWithdrawalUseCase fundWithdrawalUseCase) {
        this.fundWithdrawalUseCase = fundWithdrawalUseCase;
    }

    @Scheduled(cron = "0 0/5 9-23 ? * *") // runs by every 5 minutes from 9am to 11pm
    @SchedulerLock(name = "SavingsWithdrawalJob_processSavingsGoalWithdrawal", lockAtMostForString = "PT4M")
    public void processSavingsGoalWithdrawal(){
        try {
            fundWithdrawalUseCase.processInterestWithdrawalToSuspenseAccount();
            Thread.sleep(500);
            fundWithdrawalUseCase.processSavingsWithdrawalToSuspenseAccount();
            Thread.sleep(500);
            fundWithdrawalUseCase.processSuspenseFundDisbursementToCustomer();
        }catch (Exception ignored) { }
    }
}
