package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CorporateTransactionRequestModel {

    private String requestId;

    private String dateRequested;

    private BigDecimal amount;

    private String transactionCategory;

    private String transactionType;

    private String approvalStatus;

    private String initiatedBy;

    private String reviewedBy;

    private String transactionDescription;

    private String statusUpdatedReason;

    private String dateReviewed;

}
