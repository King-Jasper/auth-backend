package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Wed, 19 Feb, 2020
 */
@Data
public class SavingsGoalModel {
   private String goalId;
   private String name;
   private boolean autoSaveEnabled;
   private String savingPlanName;
   private double interestRate;
   private String currentStatus;
   private BigDecimal savingsBalance;
   private BigDecimal availableBalance;
   private BigDecimal savingsAmount;
   private BigDecimal targetAmount;
   private BigDecimal accruedInterest;
   private String savingFrequency;
   private String noWithdrawalErrorMessage;
   private String maturityDate;
   private String startDate;
   private String nextSavingsDate;
   private String categoryCode;
}
