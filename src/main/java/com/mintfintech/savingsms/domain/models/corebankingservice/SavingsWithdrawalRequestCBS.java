package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 29 May, 2020
 */
@Builder
@Data
public class SavingsWithdrawalRequestCBS {
    private String goalId;
    private BigDecimal amount;
    private String reference;
    private String narration;
    private String accountNumber;
    private String savingsType;
    private String withdrawalType;
}
