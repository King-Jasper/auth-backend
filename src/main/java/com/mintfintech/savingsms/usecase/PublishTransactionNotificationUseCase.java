package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
public interface PublishTransactionNotificationUseCase {
    void createTransactionLog(SavingsGoalTransactionEntity savingsGoalTransactionEntity, BigDecimal openingBalance, BigDecimal currentBalance);
    void sendSavingsFundingSuccessNotification(SavingsGoalTransactionEntity savingsGoalTransactionEntity);
    void sendSavingsFundingFailureNotification(SavingsGoalEntity goalEntity, BigDecimal savingsAmount, String failureMessage);
}
