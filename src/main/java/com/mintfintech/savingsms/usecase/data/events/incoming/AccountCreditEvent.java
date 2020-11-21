package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Sat, 21 Nov, 2020
 */
@Builder
@Data
public class AccountCreditEvent {
    private String accountNumber;
    private BigDecimal amount;
}
