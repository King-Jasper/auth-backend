package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SpendAndSaveTransactionEntity;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface SpendAndSaveTransactionDao extends CrudDao<SpendAndSaveTransactionEntity, Long> {

    Optional<SpendAndSaveTransactionEntity> findByTransactionReference(String reference);
    Page<SpendAndSaveTransactionEntity> getTransactionsBySavingsGoal(SavingsGoalEntity savingsGoal, int page, int size);
}
