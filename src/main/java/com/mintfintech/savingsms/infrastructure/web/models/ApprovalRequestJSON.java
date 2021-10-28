package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class ApprovalRequestJSON {

    @NotBlank(message = "Request Id is required.")
    private String requestId;

    @NotBlank(message = "Transaction Pin is required.")
    @Pattern(regexp = "[0-9]", message = "Transaction Pin must be digits")
    private String transactionPin;

    private boolean approved;

    private String reason;

    public CorporateApprovalRequest toRequest() {
        return CorporateApprovalRequest.builder()
                .approved(approved)
                .reason(reason)
                .requestId(requestId)
                .transactionPin(transactionPin)
                .build();
    }
}
