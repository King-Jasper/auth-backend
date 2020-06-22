package com.mintfintech.savingsms.domain.models.reports;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 22 Jun, 2020
 */
@Getter
@Setter
public class SavingsMaturityStat {

    private int day;

    private int month;

    private BigDecimal totalInterest;

    private BigDecimal totalSavings;

    public SavingsMaturityStat(int day, int month, BigDecimal totalInterest, BigDecimal totalSavings) {
        this.day = day;
        this.month = month;
        this.totalInterest = totalInterest;
        this.totalSavings = totalSavings;
    }
}
