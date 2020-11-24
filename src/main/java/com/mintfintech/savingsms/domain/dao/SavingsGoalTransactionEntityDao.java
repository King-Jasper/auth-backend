package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 31 Mar, 2020
 */
public interface SavingsGoalTransactionEntityDao extends CrudDao<SavingsGoalTransactionEntity, Long> {
    String generateTransactionReference();
    Optional<SavingsGoalTransactionEntity> findTransactionByReference(String transactionReference);
    List<SavingsGoalTransactionEntity> getTransactionByTypeAndStatusBeforeTime(TransactionTypeConstant transactionType, TransactionStatusConstant transactionStatus, LocalDateTime beforeTime, int size);
    Page<SavingsGoalTransactionEntity> getTransactions(SavingsGoalEntity goalEntity, int page, int size);
}
