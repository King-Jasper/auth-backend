package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Builder
@Data
public class MintTransactionEvent {
    private String debitAccountId;
    private String description;
    private String category;
    private String productType;
    private long spendingTagId;
    private String transactionType;
    private String internalReference;
    private String externalReference;
    private BigDecimal transactionAmount;
    private BigDecimal balanceBeforeTransaction;
    private BigDecimal balanceAfterTransaction;
    private String dateCreated;
}
