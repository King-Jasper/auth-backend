package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class LoanDeclineEmailEvent {

    private BigDecimal loanAmount;

    private String reason;

    private String recipient;

    private String customerName;
}
