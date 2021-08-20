package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

import java.util.List;

@Data
public class LoanCustomerProfileModel {

    private long id;

    private String customerName;

    private String phoneNumber;

    private String accountNumber;

    private String accountTier;

    private String email;

    private boolean blacklistStatus;

    private String blacklistReason;

    private double rating;

    private EmploymentInformationModel employmentInformation;

    private double maxLoanPercent;

    private double interestRate;

    private boolean hasActivePayDayLoan;

    private List<LoanModel> loanHistory;
}
