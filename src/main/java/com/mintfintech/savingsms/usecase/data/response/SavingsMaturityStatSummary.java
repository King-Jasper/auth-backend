package com.mintfintech.savingsms.usecase.data.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by jnwanya on
 * Mon, 22 Jun, 2020
 */
@Data
public class SavingsMaturityStatSummary {
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalInterest = BigDecimal.ZERO;
    private BigDecimal totalSavings = BigDecimal.ZERO;
    private long totalSavingsRecord = 0;
    private List<SavingsMaturityStatModel> savingsMaturityStatList;
}
