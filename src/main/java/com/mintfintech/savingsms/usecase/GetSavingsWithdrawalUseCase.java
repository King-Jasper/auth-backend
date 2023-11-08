package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.usecase.data.request.SavingsSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalWithdrawalResponse;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.inject.Named;

public interface GetSavingsWithdrawalUseCase {
    PagedDataResponse<SavingsGoalWithdrawalResponse> getSavingsGoalWithdrawalReport(SavingsSearchRequest searchRequest, int page, int size);
}
