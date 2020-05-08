package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 08 May, 2020
 */
@Builder
@Data
public class SavingsGoalWithdrawalSuccessEvent {
    private String goalName;
    private BigDecimal amount;
    private String name;
    private String recipient;
}
