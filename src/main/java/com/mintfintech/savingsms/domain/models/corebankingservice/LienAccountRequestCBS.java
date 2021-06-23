package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LienAccountRequestCBS {

    private String accountNumber;

    private String reason;

    private BigDecimal amount;

    private String referenceId;
}
