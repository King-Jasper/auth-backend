package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

import java.util.List;

@Data
public class LoanCustomerProfileModel {

    private long id;

    private String customerName;

    private String phoneNumber;

    private String email;

    private boolean blacklistStatus;

    private String blacklistReason;

    private double rating;

    private EmploymentInformationModel employmentInformation;

    private double maxLoanPercent;

    private double interestRate;

    private List<LoanModel> loanHistory;
}
