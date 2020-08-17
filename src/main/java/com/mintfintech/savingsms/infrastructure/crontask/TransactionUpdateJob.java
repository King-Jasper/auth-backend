package com.mintfintech.savingsms.infrastructure.crontask;

import com.mintfintech.savingsms.usecase.UpdateTransactionUseCase;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Sun, 02 Aug, 2020
 */
@Named
public class TransactionUpdateJob {

    private final UpdateTransactionUseCase updateTransactionUseCase;
    public TransactionUpdateJob(UpdateTransactionUseCase updateTransactionUseCase) {
        this.updateTransactionUseCase = updateTransactionUseCase;
    }

    @Scheduled(cron = "0 0/10 * ? * *") // runs by every 10 minutes
    @SchedulerLock(name = "TransactionUpdateJob_pendingTransactionUpdateTask", lockAtMostForString = "PT9M")
    public void pendingTransactionUpdateTask() {
        updateTransactionUseCase.updatePendingTransaction();
    }
}
