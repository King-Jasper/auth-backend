package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Sun, 16 Feb, 2020
 */
@Data
@Builder
public class ReferenceGenerationResponse {
    private String transactionReference;
    private BigDecimal amount;
}
