package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.FundWithdrawalUseCase;
import com.mintfintech.savingsms.usecase.features.referral_savings.ReachHQTransactionUseCase;
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
    private final ReachHQTransactionUseCase reachHQTransactionUseCase;
    public SavingsWithdrawalJob(FundWithdrawalUseCase fundWithdrawalUseCase, ReachHQTransactionUseCase reachHQTransactionUseCase) {
        this.fundWithdrawalUseCase = fundWithdrawalUseCase;
        this.reachHQTransactionUseCase = reachHQTransactionUseCase;
    }

    @Scheduled(cron = "0 0/5 9-23 ? * *") // runs by every 5 minutes from 9am to 11pm
    @SchedulerLock(name = "SavingsWithdrawalJob_processSavingsGoalWithdrawal", lockAtMostForString = "PT4M")
    public void processSavingsGoalWithdrawal(){
        try {
            fundWithdrawalUseCase.processInterestWithdrawalToSuspenseAccount();
            Thread.sleep(5000);
            fundWithdrawalUseCase.processSavingsWithdrawalToSuspenseAccount();
            Thread.sleep(5000);
            fundWithdrawalUseCase.processSuspenseFundDisbursementToCustomer();
        }catch (Exception ignored) { }
    }

  //  @Scheduled(cron = "0 0/30 9-23 ? * *") // runs by every 30 minutes from 9am to 11pm
   // @SchedulerLock(name = "SavingsWithdrawalJob_processReactHQRefund", lockAtMostForString = "PT30M")
    public void processReactHQRefund(){
       // reachHQTransactionUseCase.processCustomerCredit();
    }
}
