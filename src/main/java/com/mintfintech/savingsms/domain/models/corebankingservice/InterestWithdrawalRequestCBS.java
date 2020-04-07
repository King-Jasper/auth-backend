package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
@Builder
@Data
public class InterestWithdrawalRequestCBS {

    private String goalId;
    private String accountId;
    private String accountNumber;
    private double interestAmount;
    private String reference;
}
