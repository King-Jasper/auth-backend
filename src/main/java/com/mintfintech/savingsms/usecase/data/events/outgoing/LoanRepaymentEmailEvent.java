package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class LoanRepaymentEmailEvent {

    private BigDecimal amountPaid;

    private BigDecimal loanBalance;

    private BigDecimal transactionAmount;

    private String loanDueDate;

    private String customerName;

    private String recipient;
}
