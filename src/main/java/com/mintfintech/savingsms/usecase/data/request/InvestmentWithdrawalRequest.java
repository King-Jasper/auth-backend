package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 20 May, 2021
 */
@Builder
@Data
public class InvestmentWithdrawalRequest {
    private BigDecimal amount;
    private boolean fullLiquidation;
    private String debitAccountId;
}
