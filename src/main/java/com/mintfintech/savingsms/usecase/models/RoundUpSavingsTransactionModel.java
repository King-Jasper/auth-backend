package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Sat, 31 Oct, 2020
 */
@Data
@Builder
public class RoundUpSavingsTransactionModel {
   // private String reference;
    private BigDecimal transactionAmount;
    private BigDecimal amountSaved;
    private String dateCreated;
    private String transactionType;
}
