package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Fri, 22 May, 2020
 */
@Data
@Builder
public class NotificationPreferenceUpdateEvent {
    private String userId;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean gcmEnabled;
}
