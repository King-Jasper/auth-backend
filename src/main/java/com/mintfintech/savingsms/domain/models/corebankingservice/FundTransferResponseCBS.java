package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Data;

/**
 * Created by jnwanya on
 * Tue, 25 Feb, 2020
 */
@Data
public class FundTransferResponseCBS {
    private String responseCode;
    private String transactionReference;
    private String bankOneReference;
    private String responseMessage;
    private boolean reversed;
}
