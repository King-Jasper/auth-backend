package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 31 Mar, 2020
 */
public interface SavingsGoalTransactionEntityDao extends CrudDao<SavingsGoalTransactionEntity, Long> {
    String generateTransactionReference();
    Optional<SavingsGoalTransactionEntity> findTransactionByReference(String transactionReference);
}
