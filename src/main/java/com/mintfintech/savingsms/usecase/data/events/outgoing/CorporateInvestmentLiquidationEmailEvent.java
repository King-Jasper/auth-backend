package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class CorporateInvestmentLiquidationEmailEvent {

    private String recipient;

    private String name;

    private BigDecimal investmentAmount;

    private String maturityDate;

    private BigDecimal liquidatedAmount;

    private BigDecimal investmentBalance;

    private double penaltyRate;
}
