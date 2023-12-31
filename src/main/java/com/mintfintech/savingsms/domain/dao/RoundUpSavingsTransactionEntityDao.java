package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.RoundUpSavingsTransactionEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
public interface RoundUpSavingsTransactionEntityDao extends CrudDao<RoundUpSavingsTransactionEntity, Long> {
    Optional<RoundUpSavingsTransactionEntity> findByTransactionReference(String reference);
    Page<RoundUpSavingsTransactionEntity> getSuccessfulTransactionOnGoal(SavingsGoalEntity savingsGoalEntity, int page, int size);
}
