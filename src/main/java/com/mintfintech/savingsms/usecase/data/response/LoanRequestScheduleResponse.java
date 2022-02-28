package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by jnwanya on
 * Mon, 28 Feb, 2022
 */
@Data
@Builder
public class LoanRequestScheduleResponse {
    private BigDecimal loanAmount;
    private BigDecimal repaymentAmount;
    private double interestRate;
    private List<RepaymentSchedule> schedules;
}
