package com.mintfintech.savingsms.usecase.data.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 22 Jun, 2020
 */
@Data
public class SavingsMaturityStatModel {
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalInterest = BigDecimal.ZERO;
    private BigDecimal totalSavings = BigDecimal.ZERO;
    private String maturityDate = null;
}
