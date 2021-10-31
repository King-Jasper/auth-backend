package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class CorporateInvestmentDetailResponse {

    private BigDecimal amount;

    private int investmentDuration;

    private String initiator;

    private double interestRate;

    private String dateInitiated;

    private String maturityDate;

    private String transactionCategory;

    private BigDecimal expectedReturns;

}
