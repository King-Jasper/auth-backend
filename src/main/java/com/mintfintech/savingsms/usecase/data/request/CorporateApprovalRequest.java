package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CorporateApprovalRequest {

    private String transactionPin;

    private String requestId;

    private boolean approved;

    private String reason;

}
