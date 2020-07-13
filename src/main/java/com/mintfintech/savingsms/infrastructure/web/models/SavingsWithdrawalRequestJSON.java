package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.SavingsWithdrawalRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Created by jnwanya on
 * Sun, 05 Jul, 2020
 */
@Data
public class SavingsWithdrawalRequestJSON {
    @ApiModelProperty(notes = "The goalId to be withdrawn from.", required = true)
    @NotNull
    @NotEmpty
    private String goalId;

    @ApiModelProperty(notes = "The accountId to be credit", required = true)
    private String creditAccountId;


    public SavingsWithdrawalRequest toRequest() {
        return SavingsWithdrawalRequest.builder()
                .creditAccountId(creditAccountId)
                .amount(0.00)
                .goalId(goalId).build();
    }
}
