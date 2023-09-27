package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Tue, 26 Sep, 2023
 */
@Data
@Builder
public class HNICustomerCreationRequest {
    private double interestRate;
    private boolean chequeRequired;
    private String repaymentPlan;
    private String accountNumber;
}
