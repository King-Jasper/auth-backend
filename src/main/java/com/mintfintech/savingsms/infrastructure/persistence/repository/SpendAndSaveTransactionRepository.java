package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.dao.SpendAndSaveTransactionEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpendAndSaveTransactionRepository extends JpaRepository<SpendAndSaveTransactionEntity, Long> {
    Optional<SpendAndSaveTransactionEntity> findTopByTransactionReference(String reference);

    @Query("select s from SpendAndSaveTransactionEntity s where s.savingsGoal =:savingsGoal " +
            "order by s.dateCreated desc ")
    Page<SpendAndSaveTransactionEntity> getTransactionsBySavingsGoal(@Param("savingsGoal") SavingsGoalEntity savingsGoal, Pageable pageable);
}
