package com.mintfintech.savingsms.usecase;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
public interface ApplySavingsInterestUseCase {
    void processInterestAndUpdateGoals();
    void updateInterestLiabilityAccountWithAccumulatedInterest(BigDecimal totalAccumulatedInterest);
}
