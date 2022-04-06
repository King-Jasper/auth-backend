package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.EditSpendAndSaveRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class EditSpendAndSaveRequestJSON {

    @ApiModelProperty(notes = "Transaction percentage to be saved", required = true)
    private double transactionPercentage;

    @ApiModelProperty(notes = "This checks whether savings is locked")
    private boolean isSavingsLocked;

    @ApiModelProperty(notes = "The duration of the savings")
    private int duration;

    public EditSpendAndSaveRequest toRequest() {
        return EditSpendAndSaveRequest.builder()
                .transactionPercentage(transactionPercentage)
                .isSavingsLocked(isSavingsLocked)
                .duration(duration)
                .build();
    }
}
