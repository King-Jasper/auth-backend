package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class InvestmentCreationEmailEvent {

    private String name;
    private String recipient;
    private BigDecimal investmentAmount;
    private int duration;
    private double interestRate;
    private String maturityDate;

}
