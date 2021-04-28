package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoanProfileRejectionEmailEvent {

    private String customerName;
    private String recipient;
    private String reason;
}
