package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
@Data
@Builder
public class RoundUpSettingModel {
    private Long id;
    private String fundTransferRoundUpType;
    private String billPaymentRoundUpType;
    private String cardPaymentRoundUpType;
}
