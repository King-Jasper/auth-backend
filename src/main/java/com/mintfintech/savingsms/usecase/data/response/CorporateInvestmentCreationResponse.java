package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CorporateInvestmentCreationResponse {
    private long transferRecordId;
    private String responseCode;
    private String responseMessage;
    private String transactionReference;
    private String transactionDate;
    private BigDecimal InvestAmount;
}
