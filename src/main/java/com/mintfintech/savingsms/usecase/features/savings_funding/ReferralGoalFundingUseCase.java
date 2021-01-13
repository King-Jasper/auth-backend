package com.mintfintech.savingsms.usecase.features.savings_funding;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Wed, 13 Jan, 2021
 */
public interface ReferralGoalFundingUseCase {
    SavingsGoalFundingResponse fundReferralSavingsGoal(SavingsGoalEntity savingsGoal, BigDecimal amount);
}
