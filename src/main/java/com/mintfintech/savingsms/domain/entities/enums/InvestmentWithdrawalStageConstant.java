package com.mintfintech.savingsms.domain.entities.enums;

/**
 * Created by jnwanya on
 * Thu, 20 May, 2021
 */
public enum InvestmentWithdrawalStageConstant {

    // PENALTY_CHARGE_PAYOUT
    PENDING_INTEREST_PENALTY_CHARGE,
    PROCESSING_PRE_LIQUIDATION_PENALTY,
    FAILED_PRE_LIQUIDATION_PENALTY,


    // WITHHOLDING_TAX_PAYOUT
    PENDING_TAX_PAYMENT,
    PROCESSING_TAX_PAYMENT,
    FAILED_TAX_PAYMENT,


    // INTEREST_PAYOUT
    PENDING_INTEREST_TO_CUSTOMER,
    PROCESSING_INTEREST_TO_CUSTOMER,
    FAILED_INTEREST_TO_CUSTOMER,


    // PRINCIPAL_PAYOUT
    PENDING_PRINCIPAL_TO_CUSTOMER,
    PROCESSING_PRINCIPAL_TO_CUSTOMER,
    FAILED_PRINCIPAL_TO_CUSTOMER,
    COMPLETED,

    // FOR MATURITY
    // INTEREST_PAYOUT -> WITHHOLDING_TAX_PAYOUT -> PRINCIPAL_PAYOUT

    // FOR FULL_LIQUIDATION
    // INTEREST_PAYOUT -> PENALTY_CHARGE_PAYOUT -> WITHHOLDING_TAX_PAYOUT -> PRINCIPAL_PAYOUT

    // FOR PART_LIQUIDATION
    // PENALTY_CHARGE_PAYOUT -> PRINCIPAL_PAYOUT

    CANCELLED
}