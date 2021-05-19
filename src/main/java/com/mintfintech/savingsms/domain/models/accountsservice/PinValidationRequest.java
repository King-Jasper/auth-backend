package com.mintfintech.savingsms.domain.models.accountsservice;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PinValidationRequest {
    String transactionPin;
}
