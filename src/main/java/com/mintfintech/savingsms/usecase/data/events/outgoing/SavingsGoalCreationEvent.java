package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@Data
@Builder
public class SavingsGoalCreationEvent {
    private String goalId;
    //private String accountId;
    private String name;
    private String withdrawalAccountNumber;
    private BigDecimal savingsBalance;
    private BigDecimal accruedInterest;
}
