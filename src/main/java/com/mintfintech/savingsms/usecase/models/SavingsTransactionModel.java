package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 23 Oct, 2020
 */
@Data
@Builder
public class SavingsTransactionModel {
    private BigDecimal amount;
    private String transactionType;
    private BigDecimal savingsBalance;
    private String reference;
    private String transactionStatus;
    private boolean automated;
}
