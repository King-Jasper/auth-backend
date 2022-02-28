package com.mintfintech.savingsms.usecase.data.response;

import lombok.Data;

/**
 * Created by jnwanya on
 * Sat, 26 Feb, 2022
 */
@Data
public class LoanDashboardResponse {
    private boolean paydayLoanAvailable;
    private double payDayLoanInterest;
    private boolean businessLoanAvailable;
    private double businessLoanMonthlyInterest;
    private int businessLoanMaxMonthDuration;
    private boolean canRequestBusinessLoan;
    private int minimumDaysForReview;
    private int maximumDaysForReview;
}
