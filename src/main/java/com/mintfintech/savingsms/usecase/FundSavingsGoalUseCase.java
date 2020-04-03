package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
public interface FundSavingsGoalUseCase {
    SavingsGoalFundingResponse fundSavingGoal(MintBankAccountEntity debitAccount, AppUserEntity appUserEntity, SavingsGoalEntity savingsGoal, BigDecimal amount);
}
