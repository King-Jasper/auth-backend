package com.mintfintech.savingsms.infrastructure.web.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mintfintech.savingsms.usecase.data.request.SavingsGoalCreationRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Sun, 05 Jul, 2020
 */
@Data
public class SavingsGoalCreationRequestJSON {

    @ApiModelProperty(notes = "The savings plan Id. This is optional, defaults to tier one", required = true)
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

    @ApiModelProperty(notes = "The savings duration in days.", required = true)
    private int durationInDays;

    private boolean lockedSavings;

    @ApiModelProperty(notes = "Start Date for the savings.", required = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate startDate;

    @ApiModelProperty(notes = "Frequency Types: DAILY | WEEKLY | MONTHLY", required = false)
   // @Pattern(regexp = "(DAILY|WEEKLY|MONTHLY)", message = "Invalid frequency type.")
    private String frequency;

    @ApiModelProperty(notes = "Indicate if savings funding is automated", required = false)
    private boolean autoDebit = false;


    public SavingsGoalCreationRequest toRequest() {

        if(autoDebit) {
            if(!(frequency.equalsIgnoreCase("DAILY") || frequency.equalsIgnoreCase("WEEKLY") || frequency.equalsIgnoreCase("MONTHLY"))) {
                throw new BadRequestException("Invalid frequency type.");
            }
        }
        if(name.length() < 3) {
            throw new BadRequestException("Goal name cannot be less than 3 characters.");
        }
        if(name.length() > 25) {
            throw new BadRequestException("Goal name cannot be less than 25 characters.");
        }

        return SavingsGoalCreationRequest.builder()
                .debitAccountId(debitAccountId)
                .durationInDays(durationInDays)
                .lockedSavings(lockedSavings)
                .fundingAmount(fundingAmount)
                .name(name)
                .planId(planId)
                .categoryCode(categoryCode)
                .targetAmount(targetAmount)
                .startDate(startDate)
                .autoDebit(autoDebit)
                .frequency(frequency)
                .build();
    }
}
