package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 29 May, 2020
 */
@Data
@Builder
public class SavingsFundingRequestCBS {
    private String goalId;
    private String goalName;
    private String debitAccountNumber;
    private BigDecimal amount;
    private String narration;
    private String reference;
}
