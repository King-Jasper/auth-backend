package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 08 May, 2020
 */
@Data
@Builder
public class SavingsGoalFundingEvent {
    private String name;
    private String recipient;
    private String reference;
    private String goalName;
    private String transactionDate;
    private BigDecimal amount;
    private BigDecimal savingsBalance;
}
