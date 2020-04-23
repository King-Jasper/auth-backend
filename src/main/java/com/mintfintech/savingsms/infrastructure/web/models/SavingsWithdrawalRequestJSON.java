package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.SavingsWithdrawalRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
@Data
public class SavingsWithdrawalRequestJSON {

    @ApiModelProperty(notes = "The goalId to be withdrawn from.", required = true)
    @NotNull
    @NotEmpty
    private String goalId;

    @ApiModelProperty(notes = "The accountId to be credit", required = true)
   // @NotNull
   // @NotEmpty
    private String creditAccountId;

    @ApiModelProperty(notes = "Withdrawal amount. Used only if the goal is not matured", required = true)
    private double amount = 0;

    public SavingsWithdrawalRequest toRequest() {
         return SavingsWithdrawalRequest.builder()
                 .creditAccountId(creditAccountId)
                 .amount(amount)
                 .goalId(goalId).build();
    }
}
