package com.mintfintech.savingsms.usecase.data.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by jnwanya on
 * Tue, 06 Jul, 2021
 */
@Data
public class InvestmentMaturityStatSummary {
    private long totalRecords = 0;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalInterest = BigDecimal.ZERO;
    private BigDecimal totalInvested = BigDecimal.ZERO;
    private List<InvestmentMaturityStatModel> investmentMaturityStatList;
}
