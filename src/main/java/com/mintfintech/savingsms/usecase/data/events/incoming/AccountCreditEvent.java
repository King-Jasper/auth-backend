package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class AccountCreditEvent {
    private String accountNumber;
    private BigDecimal amount;
}
