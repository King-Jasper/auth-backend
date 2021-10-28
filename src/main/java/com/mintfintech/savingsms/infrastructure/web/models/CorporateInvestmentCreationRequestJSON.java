package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.InvestmentCreationRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

@Data
public class CorporateInvestmentCreationRequestJSON {
    @ApiModelProperty(notes = "The investment duration in months.", required = true)
    private int durationInMonths;

    @ApiModelProperty(notes = "The amount to be invested. N5000 minimum", required = true)
    @Min(value = 5000, message = "Minimum amount for investment is N5000")
    private double investmentAmount;

    @ApiModelProperty(notes = "The bank accountId to be debited", required = true)
    @NotEmpty
    private String debitAccountId;

    public InvestmentCreationRequest toRequest() {

        return InvestmentCreationRequest.builder()
                .debitAccountId(debitAccountId)
                .investmentAmount(investmentAmount)
                .durationInMonths(durationInMonths)
                .build();
    }
}
