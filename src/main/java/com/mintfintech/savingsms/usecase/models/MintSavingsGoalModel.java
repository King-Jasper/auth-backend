package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 17 Apr, 2020
 */
@Builder
@Data
public class MintSavingsGoalModel {
    private String goalId;
    private String name;
    private BigDecimal savingsBalance;
    private BigDecimal accruedInterest;
    private BigDecimal availableBalance;
    private boolean matured;
}
