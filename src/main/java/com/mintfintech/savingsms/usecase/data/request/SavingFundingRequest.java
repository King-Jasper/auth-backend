package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@Builder
@Data
public class SavingFundingRequest {
    private String goalId;
    private String debitAccountId;
    private double amount;
}
