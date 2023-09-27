package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanApplicationRequestCBS {
    private String accountNumber;
    private int amount;
    private String loanType;
    private int durationInMonths;
    private String repaymentPlanType;
}
