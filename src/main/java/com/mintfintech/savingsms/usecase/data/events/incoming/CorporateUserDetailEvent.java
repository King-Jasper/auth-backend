package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Sun, 18 Jul, 2021
 */
@Builder
@Data
public class CorporateUserDetailEvent {

    private String userId;

    private String accountId;

    private String roleName;

    private boolean director;
}
