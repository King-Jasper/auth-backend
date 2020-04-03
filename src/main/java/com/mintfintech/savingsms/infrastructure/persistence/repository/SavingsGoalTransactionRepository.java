package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 31 Mar, 2020
 */
public interface SavingsGoalTransactionRepository extends JpaRepository<SavingsGoalTransactionEntity, Long> {
    Optional<SavingsGoalTransactionEntity> findFirstByTransactionReference(String referenceReference);
}
