package com.mintfintech.savingsms.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public static void main(String[] args) {

        BigDecimal transactionAmount = BigDecimal.valueOf(200.00);
        double value = (0.5 / 100.0) * transactionAmount.doubleValue();
       // BigDecimal value = (percent.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN)).multiply(transactionAmount);
        System.out.println(" value - "+BigDecimal.valueOf(value));
    }
}
