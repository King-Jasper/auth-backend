package com.mintfintech.savingsms.domain.models;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanReviewStageConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoanSearchDTO {

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private LoanRepaymentStatusConstant repaymentStatus;
    private ApprovalStatusConstant approvalStatus;
    private LoanReviewStageConstant reviewStage;
    private MintAccountEntity account;
    private LoanTypeConstant loanType;
    private String customerName;
    private String customerPhone;
}
