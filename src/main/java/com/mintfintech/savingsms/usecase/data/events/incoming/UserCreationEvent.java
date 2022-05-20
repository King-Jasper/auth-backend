package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Data;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Data
public class UserCreationEvent {
    private String accountId;
    private String userId;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String dateCreated;
}
