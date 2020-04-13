package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Data;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Data
public class MintBankAccountCreationEvent {
    private String mintAccountId;
    private String accountId;
    private String accountType;
    private String accountGroup;
    private String accountName;
    private String accountNumber;
    private String accountTier;
    private double dailyTransactionLimit;
    private String currencyCode;
    private String dateCreated;
}
