package com.mintfintech.savingsms.usecase.data.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SpendAndSaveWithdrawalRequest {

    private double amount;

    private String creditAccountId;
}
