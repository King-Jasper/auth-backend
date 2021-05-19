package com.mintfintech.savingsms.usecase.models;

import com.mintfintech.savingsms.usecase.data.response.PortalSavingsGoalResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
public class InvestmentModel extends PortalSavingsGoalResponse {

    private String code;

    private String status;

    private String type;

    private String tenorName;

    private String maturityDate;

    private String startDate;

    private String dateWithdrawn;

    private boolean lockedInvestment;

    private double interestRate;

    private int durationInDays;

    private BigDecimal accruedInterest;

    private BigDecimal amountInvested;

    private BigDecimal totalAmountWithdrawn;

    private BigDecimal totalExpectedReturn;
}
