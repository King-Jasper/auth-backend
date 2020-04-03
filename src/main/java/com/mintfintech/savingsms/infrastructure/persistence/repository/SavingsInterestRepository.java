package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsInterestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
public interface SavingsInterestRepository extends JpaRepository<SavingsInterestEntity, Long> {
    Optional<SavingsInterestEntity> findFirstBySavingsGoalOrderByDateCreatedDesc(SavingsGoalEntity savingsGoalEntity);

    @Query("select sum(si.interest) from SavingsInterestEntity si where si.savingsGoal =:goal")
    Optional<BigDecimal> sumSavingsInterest(@Param("goal") SavingsGoalEntity savingsGoalEntity);

    long countAllBySavingsGoal(SavingsGoalEntity savingsGoalEntity);

    long countAllBySavingsGoalAndDateCreatedBetween(SavingsGoalEntity savingsGoalEntity, LocalDateTime fromTime, LocalDateTime toTime);
}
