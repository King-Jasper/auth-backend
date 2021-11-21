package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class CorporateInvestmentCreationEmailEvent {

    private String recipient;

    private String name;

    private BigDecimal investmentAmount;

    private int investmentDuration;

    private double investmentInterest;

    private String maturityDate;

}
