package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.*;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
public interface PublishTransactionNotificationUseCase {
    void createTransactionLog(SavingsGoalTransactionEntity savingsGoalTransactionEntity, BigDecimal openingBalance, BigDecimal currentBalance);
    void createTransactionLog(InvestmentEntity investmentEntity, InvestmentTransactionEntity investmentTransaction, BigDecimal openingBalance);
    void sendSavingsFundingSuccessNotification(SavingsGoalTransactionEntity savingsGoalTransactionEntity);
    void sendSavingsFundingFailureNotification(SavingsGoalEntity goalEntity, BigDecimal savingsAmount, String failureMessage);
    void sendPendingCorporateInvestmentNotification(MintAccountEntity mintAccount);
    void sendDeclinedCorporateInvestmentNotification(MintAccountEntity mintAccount);
    void publishAffiliateReferral(InvestmentEntity investment);
}
