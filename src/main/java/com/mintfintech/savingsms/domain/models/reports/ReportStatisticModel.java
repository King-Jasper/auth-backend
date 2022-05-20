package com.mintfintech.savingsms.domain.models.reports;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 18 Apr, 2022
 */
@Data
public class ReportStatisticModel {
    private long id;
    private long totalRecords;
    private BigDecimal totalAmount;

    public ReportStatisticModel(long totalRecords, BigDecimal totalAmount) {
        this.totalRecords = totalRecords;
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }
}
