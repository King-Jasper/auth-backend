package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Data;

/**
 * Created by jnwanya on
 * Sun, 31 Jul, 2022
 */
@Data
public class ProfileNameUpdateEvent {
    private String userId;
    private String firstName;
    private String lastName;
    private String middleName;
    private String accountId;
    private String accountName;
    private String bankAccountName;
}
