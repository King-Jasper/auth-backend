package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanDetailResponseCBS {

    private String loanAccountNumber;
    private BigDecimal accountBalance;
    private BigDecimal outstandingPrincipalAmount;
    private BigDecimal totalInterestPaid;
    private BigDecimal totalOutstandingAmount;
    private BigDecimal loanFeePaid;
    private BigDecimal totalAmountPaid;
    private BigDecimal loanPenaltyPaid;
}
