package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 31 Mar, 2020
 */
public interface SavingsGoalTransactionRepository extends JpaRepository<SavingsGoalTransactionEntity, Long> {
    Optional<SavingsGoalTransactionEntity> findFirstByTransactionReference(String referenceReference);
    List<SavingsGoalTransactionEntity> getAllByRecordStatusAndTransactionTypeAndTransactionStatusAndDateCreatedBefore(RecordStatusConstant rStatus,
                                                                                                  TransactionTypeConstant tType,
                                                                                TransactionStatusConstant tStatus,
                                                                                LocalDateTime createdBeforeTime,
                                                                                Pageable pageable);

    Page<SavingsGoalTransactionEntity> getAllByRecordStatusAndSavingsGoalOrderByDateCreatedDesc(RecordStatusConstant statusConstant, SavingsGoalEntity goalEntity, Pageable pageable);

}
