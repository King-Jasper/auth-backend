package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class InvestmentDetailsInfo {

    private BigDecimal amountInvested;

    private double interestRate;

    private String maturityDate;

    private BigDecimal interestAccrued;

    private BigDecimal totalExpectedReturns;
}
