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

    private FundWithdrawalUseCase fundWithdrawalUseCase;
    public SavingsWithdrawalJob(FundWithdrawalUseCase fundWithdrawalUseCase) {
        this.fundWithdrawalUseCase = fundWithdrawalUseCase;
    }

    @Scheduled(cron = "0 0/5 * ? * *") // runs by every 5 minutes
    public void processInterestAccountCrediting(){
        fundWithdrawalUseCase.processInterestCreditForFundWithdrawal();
    }

    @Scheduled(cron = "0 0/5 * ? * *") // runs by every 5 minutes
    public void processSavingAccountFunding(){
       fundWithdrawalUseCase.processSavingFundCrediting();
    }
}