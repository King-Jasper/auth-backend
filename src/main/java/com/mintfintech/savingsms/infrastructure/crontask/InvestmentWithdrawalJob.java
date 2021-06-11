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

    @Scheduled(cron = "0 0/5 * ? * *") // runs by every 5 minutes
    @SchedulerLock(name = "InvestmentWithdrawalJob_processInvestmentWithdrawal", lockAtMostForString = "PT6M")
    public void processInvestmentWithdrawal() {
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