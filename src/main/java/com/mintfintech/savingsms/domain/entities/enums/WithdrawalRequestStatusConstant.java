package com.mintfintech.savingsms.domain.entities.enums;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
public enum  WithdrawalRequestStatusConstant {
    PENDING_INTEREST_CREDIT,
    PROCESSING_INTEREST_CREDIT,
    INTEREST_CREDITING_FAILED,
    PENDING_FUND_DISBURSEMENT,
    PROCESSING_FUND_DISBURSEMENT,
    FUND_DISBURSEMENT_FAILED,
    PROCESSED,
    CANCELLED
}