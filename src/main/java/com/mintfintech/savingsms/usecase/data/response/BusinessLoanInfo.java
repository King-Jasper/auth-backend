package com.mintfintech.savingsms.usecase.data.response;

import lombok.Data;

/**
 * Created by jnwanya on
 * Fri, 25 Feb, 2022
 */
@Data
public class BusinessLoanInfo {
    private boolean canApply;
    private double interestRate;
    private PagedDataResponse<BusinessLoanResponse> loanRecords;
}
