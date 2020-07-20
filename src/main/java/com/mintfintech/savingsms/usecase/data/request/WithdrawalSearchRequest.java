package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Sun, 05 Jul, 2020
 */
@Data
@Builder
public class WithdrawalSearchRequest {
    private String goalId;
    private String autoSavedStatus;
    private String withdrawalStatus;
    private int size;
    private int pageIndex;
}
