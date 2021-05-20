package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class InvestmentSearchRequest {
    private String accountId;
    private String investmentStatus;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String investmentType;
}
