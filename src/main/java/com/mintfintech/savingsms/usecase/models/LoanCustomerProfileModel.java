package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

import java.util.List;

@Data
public class LoanCustomerProfileModel {

    private long id;

    private EmploymentInformationModel employmentInformation;

    private boolean blacklisted;

    private String blacklistReason;

    private double rating;

    private double maxLoanPercent;

    private double interestRate;

    private List<LoanModel> loans;
}
