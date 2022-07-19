package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 11 Jul, 2022
 */
@Data
@Builder
public class HairFinanceLoanResponse {
    private String loanId;
    private String dateApplied;
    private String dateDisbursed;
    private BigDecimal loanAmount;
    private int durationInMonths;
    private String dueDate;
    private String approvalStatus;
    private String repaymentStatus;
    private BigDecimal repaymentAmount;
    private BigDecimal amountPaid;
    private double interestRate;
}
