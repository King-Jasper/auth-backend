package com.mintfintech.savingsms.usecase.data.value_objects;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
public enum RoundUpTransactionCategoryType {
    FUND_TRANSFER("FUND_TRANSFER"),
    BILL_PAYMENT("BILL_PAYMENT"),
    CARD_PAYMENT("CARD_PAYMENT");

    private final String name;
    RoundUpTransactionCategoryType(String name) {
        this.name = name;
    }
    public String getCode() {
        return name;
    }

    private static final Map<String, RoundUpTransactionCategoryType> map = new HashMap<>();
    static {
        for (RoundUpTransactionCategoryType value : RoundUpTransactionCategoryType.values()) {
            map.put(value.name, value);
        }
    }
    public static RoundUpTransactionCategoryType getByName(String name) {
        return map.get(name);
    }
}
