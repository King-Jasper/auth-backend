package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Sun, 05 Jul, 2020
 */
@Data
@Builder
public class WithdrawalRequestModel {
    private String requestBy;
    private String goalName;
    private String goalId;
    private BigDecimal withdrawalAmount;
    private String creditAccountNumber;
    private String creditAccountName;
    private boolean maturedGoal;
    private String dateCreated;
    private String scheduledWithdrawalDate;
    private String transactionReference;
    private String transactionResponseCode;
    private String transactionResponseMessage;
}
