package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LoanSearchRequest {
    private String accountId;
    private String repaymentStatus;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String approvalStatus;
    private String loanType;
}
