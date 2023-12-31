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

    private boolean canLiquidate;

    private double interestRate;

    private double penaltyCharge;

    private int durationInMonths;

    private BigDecimal accruedInterest;

    private BigDecimal amountInvested;

   // private BigDecimal totalAmountInvested;

    private BigDecimal totalAmountWithdrawn;

    private BigDecimal totalAccruedInterest;

    private BigDecimal totalExpectedReturn;

    private BigDecimal estimatedProfitAtMaturity;

    private BigDecimal withholdingTax;

    private double maxLiquidateRate;

    public InvestmentModel() {
        setUserId("");
        setCustomerName("");
        setCustomerName("");
    }
}
