package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class InvestmentDetailsInfo {

    private BigDecimal amountInvested;

    private BigDecimal topUpAmount;

    private BigDecimal liquidatedAmount;

    private double interestRate;

    private String maturityDate;

    private double interestAccrued;

    private BigDecimal totalExpectedReturns;
}
