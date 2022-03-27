package com.mintfintech.savingsms.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentTransactionSearchDTO {
	private LocalDateTime fromDate;
	private LocalDateTime toDate;
	private String mintAccountNumber;
	private BigDecimal transactionAmount;
	private TransactionTypeConstant transactionType;
	private TransactionStatusConstant transactionStatus;
	private String transactionReference;
}
