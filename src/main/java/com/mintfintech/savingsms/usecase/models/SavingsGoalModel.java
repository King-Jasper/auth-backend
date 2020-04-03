package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Wed, 19 Feb, 2020
 */
@Data
@Builder
public class SavingsGoalModel {
   private String goalId;
   private String name;
   private boolean autoSaveEnabled;
   private String savingPlanName;
   private BigDecimal savingsBalance;
   private BigDecimal savingsAmount;
   private BigDecimal targetAmount;
   private BigDecimal accruedInterest;
   private String savingFrequency;
   private String maturityDate;
   private String nextSavingsDate;
}
