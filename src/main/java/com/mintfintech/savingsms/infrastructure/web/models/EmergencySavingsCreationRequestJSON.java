package com.mintfintech.savingsms.infrastructure.web.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mintfintech.savingsms.usecase.data.request.EmergencySavingsCreationRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Thu, 12 May, 2022
 */
@Data
public class EmergencySavingsCreationRequestJSON {

    @ApiModelProperty(notes = "The amount to be funded. N100 minimum", required = true)
    @Min(value = 100, message = "Minimum of N100")
    private double fundingAmount;

    @ApiModelProperty(notes = "The savings goal name", required = true)
    @NotNull
    @NotEmpty
    private String name;

    @ApiModelProperty(notes = "Start Date for the savings.", required = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate startDate;

    @ApiModelProperty(notes = "Frequency Types: DAILY | WEEKLY | MONTHLY", required = false)
    //@Pattern(regexp = "(DAILY|WEEKLY|MONTHLY)", message = "Invalid frequency type.")
    private String frequency;

    @ApiModelProperty(notes = "The bank accountId to be debited", required = true)
    @NotNull
    @NotEmpty
    private String debitAccountId;

    @ApiModelProperty(notes = "Indicate if savings funding is automated", required = true)
    private boolean autoDebit;

    public EmergencySavingsCreationRequest toRequest() {
        if(autoDebit) {
            if(!(frequency.equalsIgnoreCase("DAILY") || frequency.equalsIgnoreCase("WEEKLY") || frequency.equalsIgnoreCase("MONTHLY"))) {
                throw new BadRequestException("Invalid frequency type.");
            }
        }
        if(startDate != null && startDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Start date cannot be before current date.");
        }
        if(name.length() < 3) {
            throw new BadRequestException("Goal name cannot be less than 3 characters.");
        }
        if(name.length() > 25) {
            throw new BadRequestException("Goal name cannot be less than 25 characters.");
        }
        return EmergencySavingsCreationRequest.builder()
                .frequency(frequency)
                .fundingAmount(fundingAmount)
                .name(name)
                .startDate(startDate)
                .targetAmount(10000000)
                .autoDebit(autoDebit)
                .build();
    }
}
