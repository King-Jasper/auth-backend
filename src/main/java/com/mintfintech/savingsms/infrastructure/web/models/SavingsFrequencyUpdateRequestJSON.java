package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.SavingsFrequencyUpdateRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Data
public class SavingsFrequencyUpdateRequestJSON {

    @ApiModelProperty(notes = "Frequency Types: DAILY | WEEKLY | MONTHLY", required = true)
    @NotEmpty
    @NotNull
    @Pattern(regexp = "(DAILY|WEEKLY|MONTHLY)")
    private String frequency;
    @ApiModelProperty(notes = "Savings amount. Minimum of N100", required = true)
    @Min(100)
    private double amount;

    public SavingsFrequencyUpdateRequest toRequest(String goalId) {
        return SavingsFrequencyUpdateRequest.builder()
                .amount(amount)
                .frequency(frequency)
                .goalId(goalId).build();
    }
}
