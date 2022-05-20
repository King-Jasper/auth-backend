package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import lombok.Data;
/**
 * Created by jnwanya on
 * Wed, 19 May, 2021
 */
@Data
public class InvestmentFundingResponse {
    private String responseCode;
    private InvestmentModel investment;
    private String responseMessage;
    private String requestId;
}
