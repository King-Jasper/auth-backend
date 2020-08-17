package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Data;

/**
 * Created by jnwanya on
 * Mon, 30 Mar, 2020
 */
@Data
public class TransactionStatusResponseCBS {
    private String responseStatus;
    private String responseCode;
    private String responseMessage;
}
