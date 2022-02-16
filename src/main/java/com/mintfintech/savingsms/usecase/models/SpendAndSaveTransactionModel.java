package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class SpendAndSaveTransactionModel {

    private BigDecimal transactionAmount;

    private String dateCreated;

    private String transactionType;
}
