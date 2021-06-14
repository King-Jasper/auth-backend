package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class LoanApprovalEmailEvent {

    private String customerName;
    private String recipient;
    private BigDecimal loanRepaymentAmount;
    private String loanDueDate;
}
