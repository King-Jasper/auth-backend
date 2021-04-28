package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanModel {

    private String loanId;

    private BigDecimal loanAmount;

    private BigDecimal repaymentAmount;

    private BigDecimal amountPaid;

    private double interestRate;

    private String repaymentDueDate;

    private String repaymentStatus;

    private String approvalStatus;

    private String loanType;

    private String createdDate;

    private String approvedDate;

    private String lastPaymentDate;

    private String rejectionReason;

    private String clientLoanStatus;

    private LoanCustomerProfileModel owner;

}
