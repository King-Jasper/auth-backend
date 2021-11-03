package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class InvestmentLiquidationInfo {

    private boolean fullLiquidation;

    private BigDecimal amountToWithdraw;
}
