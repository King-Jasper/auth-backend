package com.mintfintech.savingsms.usecase.models;

import com.mintfintech.savingsms.usecase.data.response.PortalSavingsGoalResponse;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Wed, 19 Feb, 2020
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SavingsGoalModel extends PortalSavingsGoalResponse {
   private String goalId;
   private String name;
   private boolean autoSaveEnabled;
   private String savingPlanName;
   private String savingsType;
   private double interestRate;
   private String currentStatus;
   private BigDecimal savingsBalance;
   private BigDecimal availableBalance;
   private BigDecimal savingsAmount;
   private BigDecimal targetAmount;
   private BigDecimal accruedInterest;
   private BigDecimal withholdingTax;
   private String savingFrequency;
   private String noWithdrawalErrorMessage;
   private String maturityDate;
   private String startDate;
   private String nextSavingsDate;
   private String categoryCode;
   private boolean lockedSavings;

   public SavingsGoalModel() {
      setCustomerName("");
      setUserId("");
      setAccountId("");
   }
  // private int chosenSavingsDurationInDays;
}
