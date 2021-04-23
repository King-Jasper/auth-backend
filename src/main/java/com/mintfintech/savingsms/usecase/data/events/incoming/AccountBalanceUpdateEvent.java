package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Data
public class AccountBalanceUpdateEvent {
    private String accountNumber;
    private String accountId;
    private BigDecimal availableBalance;
    private BigDecimal ledgerBalance;
    private BigDecimal lienAmount;
    private BigDecimal balanceAfterLien;
}
