package com.mintfintech.savingsms.domain.models;

import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanReviewStageConstant;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerLoanProfileSearchDTO {

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private ApprovalStatusConstant verificationStatus;
    private LoanReviewStageConstant reviewStage;
    private String customerName;
    private String customerPhone;
}
