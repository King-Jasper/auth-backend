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
public class SavingsGoalCreationRequestJSONV1 {

    @ApiModelProperty(notes = "The savings plan Id", required = true)
    @NotNull
    @NotEmpty
    private String planId;

    @ApiModelProperty(notes = "The amount to be funded. N100 minimum", required = true)
    @Min(value = 100, message = "Minimum of N100")
    private double fundingAmount;

    @ApiModelProperty(notes = "The bank accountId to be debited", required = true)
    @NotNull
    @NotEmpty
    private String debitAccountId;

    @ApiModelProperty(notes = "The savings goal name", required = true)
    @NotNull
    @NotEmpty
    private String name;

    @ApiModelProperty(notes = "The savings target amount. N100 minimum", required = true)
    @Min(value = 100, message = "Minimum of N100")
    private double targetAmount;

    @ApiModelProperty(notes = "The savings category code", required = true)
    @NotNull
    @NotEmpty
    private String categoryCode;

    @ApiModelProperty(notes = "The savings plan duration Id.", required = true)
    private long durationId;

    public SavingsGoalCreationRequest toRequest() {
        return SavingsGoalCreationRequest.builder()
                .debitAccountId(debitAccountId)
                .durationId(durationId)
                .fundingAmount(fundingAmount)
                .name(name)
                .planId(planId)
                .categoryCode(categoryCode)
                .targetAmount(targetAmount).build();
    }
}
