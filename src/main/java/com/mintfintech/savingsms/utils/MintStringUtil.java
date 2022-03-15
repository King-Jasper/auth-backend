package com.mintfintech.savingsms.utils;

import java.util.Arrays;

/**
 * Created by jnwanya on
 * Tue, 01 Mar, 2022
 */
public class MintStringUtil {

    private static final String[] businessLoanAccountId =
            {"800000000227", "300001862123", "400000000609", "700000225472", "700000000592", "000001171200", "500000011553",
            "700001875063", "700001875197"};
    private static final String[] payDayLoanAccountNumber =
            {"1101146487"};

    public static boolean enableBusinessLoanFeature(String accountId) {
        return Arrays.stream(businessLoanAccountId).anyMatch(data -> data.equalsIgnoreCase(accountId));
    }

    public static boolean enablePayDayLoanFeature(String accountNumber) {
        return Arrays.stream(payDayLoanAccountNumber).anyMatch(data -> data.equalsIgnoreCase(accountNumber));
    }
}
