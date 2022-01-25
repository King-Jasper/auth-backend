package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface SpendAndSaveTransactionEntityDao extends CrudDao<SpendAndSaveTransactionEntity, Long> {

    Optional<SpendAndSaveTransactionEntity> findByTransactionReference(String reference);
    Page<SpendAndSaveTransactionEntity> getTransactionsBySavingsGoal(SavingsGoalEntity savingsGoal, int pageIndex, int size);
}
