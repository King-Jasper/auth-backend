package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentFundingRequestCBS {

    private String accountNumber;
    private String narration;
    private String transactionReference;
    private double transactionAmount;

}
