package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Data;

@Data
public class LoanApplicationResponseCBS {
    private boolean success;
    private String trackingReference;
    private String customerId;
    private String message;
}
