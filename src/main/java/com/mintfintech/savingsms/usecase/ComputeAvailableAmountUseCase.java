package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Sun, 05 Jul, 2020
 */
public interface ComputeAvailableAmountUseCase {
    BigDecimal getAvailableAmount(SavingsGoalEntity savingsGoalEntity);
    boolean isMaturedSavingsGoal(SavingsGoalEntity savingsGoalEntity);
}
