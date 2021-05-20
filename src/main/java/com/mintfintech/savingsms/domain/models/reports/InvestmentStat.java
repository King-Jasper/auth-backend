package com.mintfintech.savingsms.domain.models.reports;

import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvestmentStat {

    private InvestmentStatusConstant investmentStatus;

    private long totalRecords;

    private BigDecimal totalInvestment;

    private BigDecimal accruedInterest;

    private double outstandingInterest;

    public InvestmentStat(
            InvestmentStatusConstant investmentStatus,
            long totalRecords,
            BigDecimal totalInvestment,
            BigDecimal accruedInterest,
            double outstandingInterest
    ) {
        this.investmentStatus = investmentStatus;
        this.totalRecords = totalRecords;
        this.totalInvestment = totalInvestment;
        this.accruedInterest = accruedInterest;
        this.outstandingInterest = outstandingInterest;
    }
}
