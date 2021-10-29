package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CorporateTransactionDetailResponse {

    private double amount;

    private int investmentDuration;

    private String initiator;

    private double interestRate;

    private String dateInitiated;

    private String maturityDate;

    private String transactionCategory;

}
