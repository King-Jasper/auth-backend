package com.mintfintech.savingsms.infrastructure.web.models;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@Data
public class SavingsGoalPlanUpdateRequestJSON {

    @ApiModelProperty(notes = "The new savings plan Id", required = true)
    @NotNull
    @NotEmpty
    private String planId;
}
