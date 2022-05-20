package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import lombok.Data;

@Data
public class InvestmentCreationResponse {

    private InvestmentModel investment;

    private boolean created;

    private String message;

    private String requestId;
}
