package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 09 Apr, 2020
 */
@Builder
@Data
public class AccountLimitUpdateEvent {
    private String accountId;
    private BigDecimal dailyLimitAmount;
    private BigDecimal bulletLimitAmount;
}
