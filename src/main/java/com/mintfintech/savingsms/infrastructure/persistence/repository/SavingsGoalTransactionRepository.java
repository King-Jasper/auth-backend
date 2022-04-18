package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.models.reports.ReportStatisticModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    Optional<SavingsGoalTransactionEntity> findFirstBySavingsGoalAndTransactionStatusOrderByDateCreatedAsc(SavingsGoalEntity goalEntity, TransactionStatusConstant statusConstant);


    @Query("select new com.mintfintech.savingsms.domain.models.reports.ReportStatisticModel(count(distinct s.savingsGoal), sum(s.transactionAmount)) from SavingsGoalTransactionEntity s " +
            "where s.bankAccount.mintAccount = ?1 and s.transactionType = com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant.CREDIT and " +
            "s.transactionStatus = com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant.SUCCESSFUL")
    ReportStatisticModel getSavingsTransactionStatistics(MintAccountEntity mintAccount);

}
