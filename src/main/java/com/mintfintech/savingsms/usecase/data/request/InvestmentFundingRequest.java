package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Wed, 19 May, 2021
 */
@Data
@Builder
public class InvestmentFundingRequest {
    private String investmentCode;
    private String debitAccountId;
    private BigDecimal amount;
    private String accountId;
    private String transactionPin;
}
