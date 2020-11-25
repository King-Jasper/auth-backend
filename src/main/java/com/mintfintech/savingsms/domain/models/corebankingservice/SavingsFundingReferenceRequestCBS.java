package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Sat, 15 Feb, 2020
 */
@Data
@Builder
public class SavingsFundingReferenceRequestCBS {
    private String goalId;
    private String fundingReference;
    private long amountInNaira;
    private String paymentGateway;
}
