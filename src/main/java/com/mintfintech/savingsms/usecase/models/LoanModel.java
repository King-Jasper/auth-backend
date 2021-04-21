package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

@Data
public class LoanModel {

    private String loanId;

    private String loanAmount;

    private String repaymentAmount;

    private String amountPaid;

    private double interestRate;

    private String repaymentDueDate;

    private String repaymentStatus;

    private String approvalStatus;

    private String loanType;

    private String createdDate;

    private String approvedDate;

    private LoanCustomerProfileModel owner;

}
