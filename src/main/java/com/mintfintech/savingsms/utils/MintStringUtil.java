package com.mintfintech.savingsms.utils;

import java.util.Arrays;

/**
 * Created by jnwanya on
 * Tue, 01 Mar, 2022
 */
public class MintStringUtil {

    private static final String[] businessLoanAccountId =
            {"800000000227"};

    public static boolean enableBusinessLoanFeature(String accountId) {
        return Arrays.stream(businessLoanAccountId).anyMatch(data -> data.equalsIgnoreCase(accountId));
    }
}
