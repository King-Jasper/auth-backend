package com.mintfintech.savingsms.usecase.data.response;

import lombok.Data;

import java.math.BigDecimal;
/**
 * Created by jnwanya on
 * Tue, 06 Jul, 2021
 */
@Data
public class InvestmentMaturityStatModel {
    private long totalRecords = 0;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalInterest = BigDecimal.ZERO;
    private BigDecimal totalInvested = BigDecimal.ZERO;
    private String maturityDate = null;
}
