package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AccumulatedInterestEntity;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by jnwanya on
 * Fri, 29 May, 2020
 */
public interface AccumulatedInterestRepository extends JpaRepository<AccumulatedInterestEntity, Long> {
    List<AccumulatedInterestEntity> getAllByTransactionStatusAndDateCreatedBetween(TransactionStatusConstant transactionStatus, LocalDateTime fromDate, LocalDateTime toDate);
}
