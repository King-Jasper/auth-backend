package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Sat, 31 Oct, 2020
 */
@Builder
@Data
public class RoundUpSavingSetUpRequest {
    private String roundUpType;
    private int duration;
}
