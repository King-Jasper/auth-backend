package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoanTransactionModel {

    private String amount;

    private String status;

    private String type;

    private String reference;

    private String externalReference;

    private String responseCode;

    private String responseMessage;

    private LocalDateTime paymentDate;

}
