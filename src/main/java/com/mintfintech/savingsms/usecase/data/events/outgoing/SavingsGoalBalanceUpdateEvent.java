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
public class SavingsGoalBalanceUpdateEvent {
    private String accountId;
    private String goalId;
    private BigDecimal savingsBalance;
    private BigDecimal accruedInterest;
}
