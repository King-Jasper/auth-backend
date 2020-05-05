package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 04 May, 2020
 */
@Builder
@Data
public class TransactionReceiptEmailEvent {
    private String name;
    private String recipient;
    private String type;
    private String transactionTime;
    private String narration;
    private String reference;
    private String recipientAccountName;
    private String recipientAccountNumber;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal currentBalance;
    private String senderName;
    private String senderAccountNumber;
}
