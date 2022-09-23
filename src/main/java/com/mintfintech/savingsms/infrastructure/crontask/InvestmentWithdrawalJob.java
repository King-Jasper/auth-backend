package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.features.investment.WithdrawalInvestmentUseCase;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

@Slf4j
@Named
public class InvestmentWithdrawalJob {

    private final WithdrawalInvestmentUseCase withdrawalInvestmentUseCase;

    public InvestmentWithdrawalJob(WithdrawalInvestmentUseCase withdrawalInvestmentUseCase) {
        this.withdrawalInvestmentUseCase = withdrawalInvestmentUseCase;
    }

    @Scheduled(cron = "0 0/10 9-23 ? * *") // runs by every 10 minutes from 9am to 11pm
    @SchedulerLock(name = "InvestmentWithdrawalJob_processInvestmentWithdrawal", lockAtMostForString = "PT10M")
    public void processInvestmentWithdrawal() {
        runProcess();
    }

    @Scheduled(cron = "0 0/10 0-6 ? * *") // runs by every 10 minutes from 12am to 6am
    @SchedulerLock(name = "InvestmentWithdrawalJob_processInvestmentWithdrawalEarlyMorning", lockAtMostForString = "PT10M")
    public void processInvestmentWithdrawalEarlyMorning() {
        runProcess();
    }

    private void runProcess() {
        try {
            withdrawalInvestmentUseCase.processInterestPayout();
            Thread.sleep(500);
            withdrawalInvestmentUseCase.processPenaltyChargePayout();
            Thread.sleep(500);
            withdrawalInvestmentUseCase.processWithholdingTaxPayout();
            Thread.sleep(500);
            withdrawalInvestmentUseCase.processPrincipalPayout();
        } catch (Exception ignored) {
        }
    }
}
