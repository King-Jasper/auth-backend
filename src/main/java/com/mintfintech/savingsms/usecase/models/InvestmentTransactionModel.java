package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvestmentTransactionModel {

    private BigDecimal amount;

    private String date;

    private String type;
}
