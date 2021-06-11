package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InvestmentLiquidationEmailEvent {

    private String name;
    private String recipient;
    private BigDecimal investmentAmount;
    private BigDecimal liquidatedAmount;
    private BigDecimal penaltyCharge;
    private BigDecimal investmentBalance;
    private String maturityDate;

}
