package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
/**
 * Created by jnwanya on
 * Fri, 25 Feb, 2022
 */
@Data
@Builder
public class BusinessLoanResponse {
    private String loanId;
    private String dateApplied;
    private BigDecimal loanAmount;
    private int durationInMonths;
    private String dueDate;
    private String status;
    private BigDecimal repaymentAmount;
    private BigDecimal amountPaid;
}
