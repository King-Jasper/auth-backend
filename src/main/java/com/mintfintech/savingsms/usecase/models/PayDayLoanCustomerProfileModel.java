package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

@Data
public class PayDayLoanCustomerProfileModel {

    private long id;

    private EmploymentInformationModel employmentInformation;

    private boolean blacklisted;

    private String blacklistReason;

    private double rating;
}
