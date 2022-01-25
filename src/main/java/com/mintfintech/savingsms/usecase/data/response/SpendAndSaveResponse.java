package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.SpendAndSaveTransactionModel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
public class SpendAndSaveResponse {

    private boolean exist;

    private BigDecimal amountSaved;

    private BigDecimal accruedInterest;

    private String maturityDate;

    private String status;

    private PagedDataResponse<SpendAndSaveTransactionModel> savings;
}
