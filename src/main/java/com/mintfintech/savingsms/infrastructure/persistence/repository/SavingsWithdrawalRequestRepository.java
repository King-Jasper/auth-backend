package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsWithdrawalRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    // implemented this
    Page<SavingsWithdrawalRequestEntity> findAll(Specification<SavingsWithdrawalRequestEntity> specification, Pageable pageable);


    List<SavingsWithdrawalRequestEntity> getSavingsWithdrawalRequest(RecordStatusConstant recordStatusConstant, WithdrawalRequestStatusConstant withdrawalRequestStatusConstant, LocalDate now);
}
