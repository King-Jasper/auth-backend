package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Mon, 06 Apr, 2020
 */
@Data
@Builder
public class SavingsWithdrawalRequest {
    private String goalId;
    private double amount;
    private String creditAccountId;
}
