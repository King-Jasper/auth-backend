package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.SavingsWithdrawalRequestEntityDao;
import com.mintfintech.savingsms.usecase.GetWithdrawalRequestUseCase;
import com.mintfintech.savingsms.usecase.data.request.WithdrawalSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.WithdrawalRequestModel;
import lombok.AllArgsConstructor;
import javax.inject.Named;

/**
 * Created by jnwanya on
 * Sun, 05 Jul, 2020
 */
@Named
@AllArgsConstructor
public class GetWithdrawalRequestUseCaseImpl implements GetWithdrawalRequestUseCase {

    private final SavingsWithdrawalRequestEntityDao withdrawalRequestEntityDao;

    @Override
    public PagedDataResponse<WithdrawalRequestModel> getWithdrawalRequests(WithdrawalSearchRequest searchRequest) {
        return null;
    }
}
