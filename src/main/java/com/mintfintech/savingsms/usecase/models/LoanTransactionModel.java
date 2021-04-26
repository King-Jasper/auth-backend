package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanTransactionModel {

    private BigDecimal amount;

    private String status;

    private String type;

    private String reference;

    private String externalReference;

    private String responseCode;

    private String responseMessage;

    private String paymentDate;

}
