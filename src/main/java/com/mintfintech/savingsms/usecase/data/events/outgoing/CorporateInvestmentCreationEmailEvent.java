package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CorporateInvestmentCreationEmailEvent {

    private String recipient;

    private String name;

    private double investmentAmount;

    private int investmentDuration;

    private double investmentInterest;

    private String maturityDate;

}
