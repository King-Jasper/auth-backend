package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class InvestmentSearchRequest {
    private String accountId;
    private String investmentStatus;
    private String customerName;
    private LocalDate startFromDate;
    private LocalDate startToDate;
    private LocalDate matureFromDate;
    private LocalDate matureToDate;
    private boolean completedRecords;
    private int duration;
    private String accountType;
}
