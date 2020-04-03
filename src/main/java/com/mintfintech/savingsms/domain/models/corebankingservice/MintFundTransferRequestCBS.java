package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Tue, 25 Feb, 2020
 */
@Builder
@Data
public class MintFundTransferRequestCBS {
    private String transactionReference;
    private String debitAccountNumber;
    private String creditAccountNumber;
    private BigDecimal amount;
    private String narration;
}
