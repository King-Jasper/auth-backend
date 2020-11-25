package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 22 Oct, 2020
 */
@Builder
@Data
public class OnlineFundingResponse {
    private String goalId;
    private String transactionReference;
    private BigDecimal amount;
    private String paymentGateway;
    private String paymentStatus;
    private String transactionDate;
}
