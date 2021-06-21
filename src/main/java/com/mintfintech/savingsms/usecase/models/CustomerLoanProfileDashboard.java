package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

@Data
public class CustomerLoanProfileDashboard {

    private LoanCustomerProfileModel customerProfile;

    private boolean hasCustomerProfile;

}
