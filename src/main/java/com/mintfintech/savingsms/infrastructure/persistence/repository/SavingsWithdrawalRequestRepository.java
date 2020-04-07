package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsWithdrawalRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
public interface SavingsWithdrawalRequestRepository extends JpaRepository<SavingsWithdrawalRequestEntity, Long> {
    long countAllBySavingsGoalAndDateCreatedBetween(SavingsGoalEntity savingsGoalEntity, LocalDateTime fromTime, LocalDateTime toTime);
    List<SavingsWithdrawalRequestEntity> getAllByRecordStatusAndWithdrawalRequestStatusOrderByDateModifiedAsc(RecordStatusConstant statusConstant, WithdrawalRequestStatusConstant requestStatusConstant);

}
