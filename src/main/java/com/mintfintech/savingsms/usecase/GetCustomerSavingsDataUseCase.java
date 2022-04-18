package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.usecase.data.response.CustomerSavingsStatisticResponse;

/**
 * Created by jnwanya on
 * Mon, 18 Apr, 2022
 */
public interface GetCustomerSavingsDataUseCase {
    CustomerSavingsStatisticResponse getCustomerSavingsStatistics(String accountId);
}
