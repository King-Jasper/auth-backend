package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.SavingsWithdrawalRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class SavingsWithdrawalRequestJSONV2 {
    @ApiModelProperty(notes = "The goalId to be withdrawn from.", required = true)
    @NotNull
    @NotEmpty
    private String goalId;

    @ApiModelProperty(notes = "The accountId to be credit", required = true)
    private String creditAccountId;

    @ApiModelProperty(notes = "The amount to be withdrawn", required = true)
    private double amount;


    public SavingsWithdrawalRequest toRequest() {
        return SavingsWithdrawalRequest.builder()
                .creditAccountId(creditAccountId)
                .amount(amount)
                .goalId(goalId).build();
    }
}
