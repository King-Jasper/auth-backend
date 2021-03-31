package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoanModel {

    private String loanId;

    private String loanAmount;

    private String repaymentAmount;

    private String amountPaid;

    private double interestRate;

    private LocalDateTime repaymentDueDate;

    private String repaymentStatus;

    private String approvalStatus;

    private String loanType;

    private LoanCustomerProfileModel owner;

    private List<LoanTransactionModel> transactions;
}
