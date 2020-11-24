package com.mintfintech.savingsms.domain.entities.enums;

/**
 * Created by jnwanya on
 * Fri, 23 Oct, 2020
 */
public enum RoundUpSavingsTypeConstant {
    NONE("None"),
    NEAREST_TEN("Nearest Ten"),
    NEAREST_HUNDRED("Nearest Hundred"),
    NEAREST_THOUSAND("Nearest Thousand");
    private final String name;
    RoundUpSavingsTypeConstant(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
