package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.ApplySavingsInterestUseCase;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@Named
public class SavingsInterestJob {

    private final ApplySavingsInterestUseCase applySavingsInterestUseCase;
    public SavingsInterestJob(ApplySavingsInterestUseCase applySavingsInterestUseCase) {
        this.applySavingsInterestUseCase = applySavingsInterestUseCase;
    }

    @Scheduled(cron = "0 30 23 ? * *") // runs by 11:30PM every day 23
    @SchedulerLock(name = "SavingsInterestJob_processInterestApplication")
    public void processInterestApplication() {
         applySavingsInterestUseCase.processInterestAndUpdateGoals();
    }
}
