package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SavingsSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.AccountSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;

import java.util.List;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
public interface GetSavingsGoalUseCase {
    SavingsGoalModel fromSavingsGoalEntityToModel(SavingsGoalEntity savingsGoalEntity);
    SavingsGoalModel getSavingsGoalByGoalId(AuthenticatedUser authenticatedUser, String goalId);
    List<SavingsGoalModel> getSavingsGoalList(MintAccountEntity mintAccountEntity);
    List<SavingsGoalModel> getSavingsGoalList(AuthenticatedUser authenticatedUser);
    AccountSavingsGoalResponse getAccountSavingsGoals(AuthenticatedUser authenticatedUser);
    PagedDataResponse<SavingsGoalModel> getPagedSavingsGoals(SavingsSearchRequest searchRequest, int page, int size);
}
