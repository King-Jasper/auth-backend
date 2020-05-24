package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Sun, 24 May, 2020
 */
@Builder
@Data
public class PlanChangeRequest {
    private String planId;
    private Long durationId;
}
