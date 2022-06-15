package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Tue, 16 Mar, 2021
 */
@Data
@Builder
public class UserDetailUpdateEvent {
    private String userId;
    private String phoneNumber;
    private String email;
    private String gender;
    private String address;
    private String username;
}
