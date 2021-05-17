package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Thu, 08 Apr, 2021
 */
@Builder
@Data
public class RoundUpTypeUpdateRequest {
    private String fundTransferRoundUpType;
    private String billPaymentRoundUpType;
}
