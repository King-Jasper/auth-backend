package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

@Data
public class InvestmentCreationResponseModel {

    private InvestmentModel investment;

    private boolean created;

    private String message;
}
