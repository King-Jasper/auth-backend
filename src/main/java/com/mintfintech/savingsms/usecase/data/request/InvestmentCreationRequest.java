package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentCreationRequest {

    private String tenorId;

    private double investmentAmount;

    private String debitAccountId;

    private String transactionPin;
}
