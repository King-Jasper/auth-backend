package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.InvestmentCreationRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@Data
public class InvestmentCreationRequestJSON {

    @ApiModelProperty(notes = "The investment tenor Id.", required = true)
    @NotEmpty
    private String tenorId;

    @ApiModelProperty(notes = "The amount to be invested. N5000 minimum", required = true)
    @Min(value = 5000, message = "Minimum of N5000")
    private double investmentAmount;

    @ApiModelProperty(notes = "The bank accountId to be debited", required = true)
    @NotEmpty
    private String debitAccountId;

    @ApiModelProperty(notes = "Transaction Pin")
    @Pattern(regexp = "[0-9]{4}")
    private String transactionPin;

    public InvestmentCreationRequest toRequest() {

        return InvestmentCreationRequest.builder()
                .debitAccountId(debitAccountId)
                .investmentAmount(investmentAmount)
                .tenorId(tenorId)
                .transactionPin(transactionPin)
                .build();
    }
}
