package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class InvestmentFundingEmailEvent {

    private String name;
    private String recipient;
    private BigDecimal amount;
    private BigDecimal investmentBalance;
    private int duration;
    private double interestRate;
    private String maturityDate;
}
