package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.usecase.data.request.WithdrawalSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.WithdrawalRequestModel;

/**
 * Created by jnwanya on
 * Sun, 05 Jul, 2020
 */
public interface GetWithdrawalRequestUseCase {
    PagedDataResponse<WithdrawalRequestModel> getWithdrawalRequests(WithdrawalSearchRequest searchRequest);
}
