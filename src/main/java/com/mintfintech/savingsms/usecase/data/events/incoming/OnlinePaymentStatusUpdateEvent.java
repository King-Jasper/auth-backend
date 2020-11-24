package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Data;

/**
 * Created by jnwanya on
 * Thu, 22 Oct, 2020
 */
@Data
public class OnlinePaymentStatusUpdateEvent {
    private String transactionReference;
    private long amount;
    private String paymentStatus;
    private String paymentDate;
}
