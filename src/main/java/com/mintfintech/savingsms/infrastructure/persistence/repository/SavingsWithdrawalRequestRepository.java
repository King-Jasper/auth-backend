package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsWithdrawalRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
public interface SavingsWithdrawalRequestRepository extends JpaRepository<SavingsWithdrawalRequestEntity, Long> {
    long countAllBySavingsGoalAndDateCreatedBetween(SavingsGoalEntity savingsGoalEntity, LocalDateTime fromTime, LocalDateTime toTime);
    @Query("select sw from SavingsWithdrawalRequestEntity sw where sw.recordStatus =?1 and sw.withdrawalRequestStatus = ?2 and (sw.dateForWithdrawal is null " +
            "or sw.dateForWithdrawal <= ?3) order by sw.dateModified desc")
    List<SavingsWithdrawalRequestEntity> getSavingsWithdrawalRequest(RecordStatusConstant statusConstant, WithdrawalRequestStatusConstant requestStatusConstant,
                                                                                                              LocalDate dateForWithdrawal);

}
