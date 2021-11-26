package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class CorporateInvestmentTopUpDetailResponse {

    private BigDecimal amountInvested;

    private int investmentDuration;

    private String initiator;

    private double interestRate;

    private String dateInitiated;

    private String maturityDate;

    private String transactionCategory;

    private BigDecimal topUpAmount;

    private double interestAccrued;

    private BigDecimal totalExpectedReturns;

    private String approvalStatus;

    private String dateReviewed;

    private String reviewedBy;

}
