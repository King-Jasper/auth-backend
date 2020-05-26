package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Sat, 16 May, 2020
 */
@Data
@Builder
public class SmsLogEvent {
    private String accountId;
    private String userId;
    private String phoneNumber;
    private boolean charged;
    private String message;
}
