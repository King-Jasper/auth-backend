package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import lombok.Data;

@Data
public class InvestmentLiquidationResponse {

    private String message;

    private InvestmentModel investmentModel;

    private String requestId;
}
