package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.InvestmentCreationRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * Created by jnwanya on
 * Fri, 09 Jul, 2021
 */
@Data
public class InvestmentCreationAdminRequestJSON {

    @ApiModelProperty(notes = "The investment duration in months.", required = true)
    private int durationInMonths;

    @ApiModelProperty(notes = "The amount to be invested. N5000 minimum", required = true)
    @Min(value = 5000, message = "Minimum amount for investment is N5000")
    private double investmentAmount;

    @ApiModelProperty(notes = "The bank accountId to be debited", required = true)
    @NotEmpty
    private String debitAccountId;

    @ApiModelProperty(notes = "User Id of the person")
    @NotEmpty(message = "UserId is required.")
    private String userId;

    public InvestmentCreationRequest toRequest() {

        return InvestmentCreationRequest.builder()
                .debitAccountId(debitAccountId)
                .investmentAmount(investmentAmount)
                .durationInMonths(durationInMonths)
                .userId(userId)
                .build();
    }
}
