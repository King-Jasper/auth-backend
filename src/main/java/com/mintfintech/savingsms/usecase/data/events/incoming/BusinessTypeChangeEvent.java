package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Data;

/**
 * Created by jnwanya on
 * Sun, 31 Jul, 2022
 */
@Data
public class BusinessTypeChangeEvent {
    private String accountId;
    private String newBusinessType;
}
