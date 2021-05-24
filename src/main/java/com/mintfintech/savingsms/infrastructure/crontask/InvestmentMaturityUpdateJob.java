package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.features.investment.UpdateInvestmentMaturityUseCase;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

@Slf4j
@Named
public class InvestmentMaturityUpdateJob {

    private final UpdateInvestmentMaturityUseCase updateInvestmentMaturityUseCase;

    public InvestmentMaturityUpdateJob(UpdateInvestmentMaturityUseCase updateInvestmentMaturityUseCase) {
        this.updateInvestmentMaturityUseCase = updateInvestmentMaturityUseCase;
    }

    @Scheduled(cron = "0 0 0/2 ? * *") // runs every 2 hours every day
    @SchedulerLock(name = "InvestmentMaturityUpdateJob_processInvestmentMaturityUpdate", lockAtMostForString = "PT30M")
    public void processInvestmentMaturityUpdate() {
        log.info("cron task processInvestmentMaturityUpdate");
        updateInvestmentMaturityUseCase.updateStatusForMaturedInvestment();
    }
}
