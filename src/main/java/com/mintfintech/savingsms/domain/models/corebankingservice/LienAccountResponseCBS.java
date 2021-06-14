package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Data;

@Data
public class LienAccountResponseCBS {

    private String accountNumber;

    private String referenceID;

    private boolean success;

    private String status;

    private String message;
}
