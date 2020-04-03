package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.SavingsGoalCreationRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Data
public class SavingsGoalCreationRequestJSON {

    @ApiModelProperty(notes = "The savings plan Id", required = true)
    @NotNull
    @NotEmpty
    private String planId;

    @ApiModelProperty(notes = "The amount to be funded.", required = true)
    @Min(100)
    private double fundingAmount;

    @ApiModelProperty(notes = "The bank accountId to be debited", required = true)
    @NotNull
    @NotEmpty
    private String debitAccountId;

    @ApiModelProperty(notes = "The savings goal name", required = true)
    @NotNull
    @NotEmpty
    private String name;

    @ApiModelProperty(notes = "The savings target amount.", required = true)
    @Min(100)
    private double targetAmount;

    @ApiModelProperty(notes = "The savings plan duration Id.", required = true)
    private long durationId;

    public SavingsGoalCreationRequest toRequest() {
        return SavingsGoalCreationRequest.builder()
                .debitAccountId(debitAccountId)
                .durationId(durationId)
                .fundingAmount(fundingAmount)
                .name(name)
                .planId(planId)
                .targetAmount(targetAmount).build();
    }
}
