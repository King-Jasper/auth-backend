package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Data
public class BalanceEnquiryResponseCBS {
    private String accountNumber;
    private BigDecimal availableBalance;
    private BigDecimal ledgerBalance;
}
