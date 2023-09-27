package com.mintfintech.savingsms.domain.models.reports;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentPlanTypeConstant;
import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Tue, 26 Sep, 2023
 */
@Data
@Builder
public class HNICustomerSearchDTO {
    private MintAccountEntity mintAccount;
    private LoanRepaymentPlanTypeConstant repaymentPlanType;
    private String customerName;
}
