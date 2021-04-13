package com.mintfintech.savingsms.domain.models.corebankingservice;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class LoanTransactionRequestCBS {

    private String loanId;
    private BigDecimal amount;
    private String reference;
    private String narration;
    private String accountNumber;
    private String loanTransactionType;
    private BigDecimal fee;
}
