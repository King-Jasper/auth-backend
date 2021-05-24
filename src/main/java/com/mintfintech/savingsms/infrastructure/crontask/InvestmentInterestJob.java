package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.features.investment.ApplyInvestmentInterestUseCase;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

@Named
public class InvestmentInterestJob {

    private final ApplyInvestmentInterestUseCase applyInvestmentInterestUseCase;

    public InvestmentInterestJob(ApplyInvestmentInterestUseCase applyInvestmentInterestUseCase) {
        this.applyInvestmentInterestUseCase = applyInvestmentInterestUseCase;
    }

    @SchedulerLock(name = "InvestmentInterestJob_runProcessInvestmentInterestApplication", lockAtMostForString = "PT45M")
    @Scheduled(cron = "0 50 23 1/1 * ?") // runs every day at 11:50pm.
    public void processInvestmentInterestApplication() {
        applyInvestmentInterestUseCase.processAndApplyInterest();
    }

}
