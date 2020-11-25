package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.EmergencySavingModel;
import com.mintfintech.savingsms.usecase.models.MintSavingsGoalModel;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by jnwanya on
 * Fri, 17 Apr, 2020
 */
@Builder
@Data
public class AccountSavingsGoalResponse {
    private List<SavingsGoalModel> customerGoals;
    private List<MintSavingsGoalModel> mintGoals;
    private EmergencySavingModel emergencySaving;
}
