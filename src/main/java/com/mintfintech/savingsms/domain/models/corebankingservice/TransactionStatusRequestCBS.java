package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 30 Mar, 2020
 */
@Data
@Builder
public class TransactionStatusRequestCBS {
    private String transactionDate;
    private BigDecimal amount;
    private String transactionReference;
    private boolean nipTransfer;
}
