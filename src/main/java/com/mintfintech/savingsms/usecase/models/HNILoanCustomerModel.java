package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Tue, 26 Sep, 2023
 */
@Data
@Builder
public class HNILoanCustomerModel {
    private String dateProfiled;
    private String customerName;
    private String accountNumber;
    private String repaymentType;
    private double interestRate;
    private boolean chequeRequired;
    private String lastUpdatedBy;
}
