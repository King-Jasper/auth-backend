package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.features.roundup_savings.UpdateRoundUpSavingsUseCase;
import lombok.AllArgsConstructor;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Sat, 31 Oct, 2020
 */
@Named
@AllArgsConstructor
public class CleanUpJob {

    private final UpdateRoundUpSavingsUseCase updateRoundUpSavingsUseCase;

    @SchedulerLock(name = "CleanUpJob_roundUpSavingsCleanUp", lockAtMostForString = "PT5M")
    @Scheduled(fixedDelay = 1000 * 60 * 60, initialDelay = 1000 * 60 * 10)
    public void roundUpSavingsCleanUp() {
        updateRoundUpSavingsUseCase.deleteDeactivatedRoundUpSavingsWithZeroBalance();;
    }
}
