package com.mintfintech.savingsms.domain.entities.enums;

/**
 * Created by jnwanya on
 * Tue, 26 Sep, 2023
 */
public enum LoanRepaymentPlanTypeConstant {
    PRORATED_PRINCIPAL_INTEREST("PRO-RATED PRINCIPAL AND INTEREST"),
    END_OF_TENURE("END OF TENURE"),
    INTEREST_ONLY("INTEREST ONLY")
    ;
    final String type;
    LoanRepaymentPlanTypeConstant(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
    }
}
