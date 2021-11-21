package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class CorporateInvestmentTopUpEmailEvent {

    private String recipient;

    private String name;

    private BigDecimal investmentAmount;

    private int investmentDuration;

    private double investmentInterestRate;

    private String maturityDate;

    private BigDecimal investmentInterestAccruedTillDate;

    private BigDecimal topUpAmount;

    private BigDecimal expectedReturns;

}
