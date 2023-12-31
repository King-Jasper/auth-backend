package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.SpendAndSaveTransactionModel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SpendAndSaveResponse {

    private boolean exist;

    private BigDecimal amountSaved;

    private BigDecimal accruedInterest;

    private String maturityDate;

    private String status;

    private PagedDataResponse<SpendAndSaveTransactionModel> savings;

    private boolean isSavingsLocked;

    private BigDecimal totalAmount;

    private double percentage;

    private boolean isSavingsMature;
}
