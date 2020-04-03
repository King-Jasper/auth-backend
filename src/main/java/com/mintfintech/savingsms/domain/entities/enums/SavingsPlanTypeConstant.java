package com.mintfintech.savingsms.domain.entities.enums;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public enum SavingsPlanTypeConstant {
    SAVINGS_TIER_ONE("Savings Tier One"),
    SAVINGS_TIER_TWO("Savings Tier Two"),
    SAVINGS_TIER_THREE("Savings Tier Three");
    private String name;
    SavingsPlanTypeConstant(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
