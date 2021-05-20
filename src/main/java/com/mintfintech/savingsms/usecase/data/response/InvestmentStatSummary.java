package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvestmentStatSummary {

    private PagedDataResponse<InvestmentModel> investments;

    private BigDecimal totalExpectedReturns = BigDecimal.ZERO;

    private BigDecimal totalInvested = BigDecimal.ZERO;

    private BigDecimal totalProfit = BigDecimal.ZERO;

    private long totalInvestments = 0;

    private long totalActiveInvestment = 0;

}
