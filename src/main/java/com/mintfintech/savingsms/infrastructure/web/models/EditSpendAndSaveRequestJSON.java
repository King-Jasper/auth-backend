package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.EditSpendAndSaveRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class EditSpendAndSaveRequestJSON {

    @ApiModelProperty(notes = "Transaction percentage to be saved", required = true)
    @NotBlank(message = "Transaction percentage is mandatory")
    private int transactionPercentage;

    @ApiModelProperty(notes = "This checks whether savings is locked")
    @NotBlank(message = "isSavingsLocked is mandatory")
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
