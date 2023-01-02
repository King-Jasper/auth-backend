package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.usecase.data.response.InterestUpdateResponse;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
public interface ApplySavingsInterestUseCase {
    void processInterestAndUpdateGoals();
    void updateInterestLiabilityAccountWithAccumulatedInterest(BigDecimal totalAccumulatedInterest);
    InterestUpdateResponse recalculateInterestOnSavings(String goalId, boolean updateInterest);
}
