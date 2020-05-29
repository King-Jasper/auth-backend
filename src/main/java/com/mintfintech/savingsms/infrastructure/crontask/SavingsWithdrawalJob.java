package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.FundWithdrawalUseCase;
import lombok.extern.slf4j.Slf4j;
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

    @Scheduled(cron = "0 0/5 * ? * *") // runs by every 5 minutes
    public void processSavingsGoalWithdrawal(){
        fundWithdrawalUseCase.processInterestWithdrawalToSuspenseAccount();
        //Thread.sleep(1000);
        fundWithdrawalUseCase.processSavingsWithdrawalToSuspenseAccount();
        //Thread.sleep(1000);
        fundWithdrawalUseCase.processSuspenseFundDisburseToCustomer();
    }
}
