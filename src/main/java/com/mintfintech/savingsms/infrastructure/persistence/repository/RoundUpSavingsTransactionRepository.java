package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.RoundUpSavingsTransactionEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
public interface RoundUpSavingsTransactionRepository extends JpaRepository<RoundUpSavingsTransactionEntity, Long> {
    Optional<RoundUpSavingsTransactionEntity> findTopByTransactionReference(String reference);

    @Query("select r from RoundUpSavingsTransactionEntity r where r.savingsGoal = ?1 and r.transaction is not null and " +
            "r.transaction.transactionStatus = com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant.SUCCESSFUL " +
            "order by r.dateCreated desc ")
    Page<RoundUpSavingsTransactionEntity> getSuccessfulTransactions(SavingsGoalEntity goalEntity, Pageable pageable);
}
