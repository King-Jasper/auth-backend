package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.RoundUpSavingsTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
public interface RoundUpSavingsTransactionRepository extends JpaRepository<RoundUpSavingsTransactionEntity, Long> {
    Optional<RoundUpSavingsTransactionEntity> findTopByTransactionReference(String reference);
}
