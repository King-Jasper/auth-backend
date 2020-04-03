package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.SavingFundingRequest;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
public class SavingFundingRequestJSON {

    @ApiModelProperty(notes = "The goalId to be funded", required = true)
    @NotNull
    @NotEmpty
    private String goalId;

    @ApiModelProperty(notes = "The accountId to be debited", required = true)
    @NotNull
    @NotEmpty
    private String debitAccountId;
    @ApiModelProperty(notes = "Savings amount. Minimum of N100", required = true)
    @Min(100)
    private double amount;

    public SavingFundingRequest toRequest() {
        return SavingFundingRequest.builder()
                .amount(amount)
                .debitAccountId(debitAccountId)
                .goalId(goalId).build();
    }
}
