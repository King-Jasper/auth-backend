package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsInterestEntity;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
public interface SavingsInterestEntityDao extends CrudDao<SavingsInterestEntity, Long> {
    BigDecimal getTotalInterestAmountOnGoal(SavingsGoalEntity savingsGoalEntity);
    long countInterestOnGoal(SavingsGoalEntity savingsGoalEntity);
    long countInterestOnGoal(SavingsGoalEntity savingsGoalEntity, LocalDateTime fromTime, LocalDateTime toTime);
    Optional<SavingsInterestEntity> findLastInterestApplied(SavingsGoalEntity savingsGoalEntity);
    Optional<SavingsInterestEntity> findFirstInterestApplied(SavingsGoalEntity savingsGoalEntity);
    Page<SavingsInterestEntity> getAccruedInterestOnGoal(SavingsGoalEntity goalEntity, int page, int size);
}
