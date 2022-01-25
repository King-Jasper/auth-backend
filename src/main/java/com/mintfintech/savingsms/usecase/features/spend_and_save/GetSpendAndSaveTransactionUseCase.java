package com.mintfintech.savingsms.usecase.features.spend_and_save;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.SpendAndSaveTransactionModel;

public interface GetSpendAndSaveTransactionUseCase {
    PagedDataResponse<SpendAndSaveTransactionModel> getSpendAndSaveTransactions(SavingsGoalEntity savingsGoal);
}
