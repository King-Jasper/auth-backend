package com.mintfintech.savingsms.domain.models;

import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoanSearchDTO {

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private LoanRepaymentStatusConstant status;
    private LoanApprovalStatusConstant approvalStatus;
    private MintBankAccountEntity account;
    private LoanTypeConstant loanType;
}
