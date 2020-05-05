package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Tue, 05 May, 2020
 */
@Builder
@Data
public class SavingsGoalFundingFailureEvent {
    private String name;
    private String type;
    private String recipient;
    private BigDecimal amount;
    private String status;
    private String failureMessage;
}
