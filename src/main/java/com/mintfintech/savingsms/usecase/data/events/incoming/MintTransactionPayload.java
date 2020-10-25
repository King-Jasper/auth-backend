package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
/**
 * Created by jnwanya on
 * Wed, 01 Apr, 2020
 */
@Builder
@Data
public class MintTransactionPayload {
    private String debitAccountId;
    private String description;
    private String category;
    private long spendingTagId;
    private String tagCode;
    private String transactionType;
    private String internalReference;
    private String externalReference;
    private BigDecimal transactionAmount;
    private BigDecimal balanceBeforeTransaction;
    private BigDecimal balanceAfterTransaction;
    private String dateCreated;
}
