package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentWithdrawalRequestCBS {

    private String narration;
    private String accountNumber;
    private double transactionAmount;
    private String transactionReference;
    private String withdrawalType;
    private String withdrawalStage;
}
