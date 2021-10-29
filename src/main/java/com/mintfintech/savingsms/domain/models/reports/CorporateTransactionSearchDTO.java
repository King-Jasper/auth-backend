package com.mintfintech.savingsms.domain.models.reports;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionApprovalStatusConstant;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class CorporateTransactionSearchDTO {

    private MintAccountEntity corporate;

    private LocalDate fromDate;

    private LocalDate toDate;

    private CorporateTransactionTypeConstant transactionType;

    private TransactionApprovalStatusConstant approvalStatus;

}
