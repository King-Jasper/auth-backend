package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 18 Apr, 2022
 */
@Data
@Builder
public class CustomerSavingsStatisticResponse {
    private long totalSavingsGoal;
    private BigDecimal totalAmountSaved;
    private long totalInvestment;
    private BigDecimal totalAmountInvested;
}
