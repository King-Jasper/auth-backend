package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Data
@Builder
public class SavingsGoalFundingResponse {
    private String responseCode;
    private String responseMessage;
    private String transactionReference;
    private BigDecimal savingsBalance;
}
