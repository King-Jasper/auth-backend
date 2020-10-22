package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Thu, 22 Oct, 2020
 */
@Builder
@Data
public class OnlineFundingRequest {
    private String gaolId;
    private long amount;
    private String paymentGateway;
}
