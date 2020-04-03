package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Data
public class MintAccountCreationEvent {
    private String accountId;
    private String name;
    private String accountType;
    private BigDecimal dailyTransactionLimit;
    private UserCreationEvent userCreationEvent;
}
