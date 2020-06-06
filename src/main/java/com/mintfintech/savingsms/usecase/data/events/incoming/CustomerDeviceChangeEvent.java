package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Data;

/**
 * Created by jnwanya on
 * Mon, 10 Feb, 2020
 */
@Data
public class CustomerDeviceChangeEvent {
    private String customerId;
    private String deviceUniqueId;
    private String deviceNotificationId;
    private String deviceModel;
}
