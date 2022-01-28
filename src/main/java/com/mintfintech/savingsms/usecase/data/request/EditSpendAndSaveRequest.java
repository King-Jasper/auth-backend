package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EditSpendAndSaveRequest {

    private int transactionPercentage;

    private boolean isSavingsLocked;

    private int duration;
}
