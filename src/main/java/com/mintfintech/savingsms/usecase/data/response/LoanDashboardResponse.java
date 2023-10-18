package com.mintfintech.savingsms.usecase.data.response;

import lombok.Data;
import java.util.List;

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
    private boolean canRequestBusinessLoan;
    private boolean chequeUploadRequired;
    private int minimumDaysForReview;
    private int maximumDaysForReview;
    private List<LoanDuration> businessLoanDurations;
}
