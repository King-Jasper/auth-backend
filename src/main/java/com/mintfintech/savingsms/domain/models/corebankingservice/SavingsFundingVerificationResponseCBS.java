package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Data;

/**
 * Created by jnwanya on
 * Thu, 22 Oct, 2020
 */
@Data
public class SavingsFundingVerificationResponseCBS {
    private String transactionReference;
    private String goalId;
    private long amount;
    private String paymentStatus;
    private String paymentDate;
}
