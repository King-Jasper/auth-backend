package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentCreationRequest {

    private long durationId;

    private int durationInMonths;

    private double investmentAmount;

    private String debitAccountId;

    private String transactionPin;

    private String userId;

    private String referralCode;
}
