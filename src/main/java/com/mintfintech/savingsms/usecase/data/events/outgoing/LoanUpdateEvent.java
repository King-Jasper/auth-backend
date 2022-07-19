package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Mon, 11 Jul, 2022
 */
@Data
@Builder
public class LoanUpdateEvent {
    public enum UpdateType {
        DECLINED, APPROVED, DISBURSED, REPAID
    }
    private String loanId;
    private String loanType;
    private String updateType;
}
