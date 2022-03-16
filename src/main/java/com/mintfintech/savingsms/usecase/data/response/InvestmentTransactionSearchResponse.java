package com.mintfintech.savingsms.usecase.data.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentTransactionSearchResponse {
	private String transactionDate;
	private BigDecimal investmentBalance;
	private BigDecimal transactionAmount;
	private String customerName;
	private String customerAccountNumber;
	private String transactionType;
	private String transactionStatus;
	private String responseCode;
	private String responseMessage;
}
