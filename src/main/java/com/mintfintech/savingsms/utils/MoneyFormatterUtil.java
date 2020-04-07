package com.mintfintech.savingsms.utils;

import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
public class MoneyFormatterUtil {

    public static String priceWithDecimal(BigDecimal price) {
        DecimalFormat formatter = new DecimalFormat("###,###,###.00");
        formatter.setRoundingMode(RoundingMode.CEILING);
        return formatter.format(price);
    }

    public static String priceWithDecimal(Double price) {
        DecimalFormat formatter = new DecimalFormat("###,###,###.00");
        formatter.setRoundingMode(RoundingMode.CEILING);
        return formatter.format(price);
    }

    public static String priceWithoutDecimal(Double price) {
        NumberFormat format = DecimalFormat.getInstance();
        format.setRoundingMode(RoundingMode.FLOOR);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(0);
        return format.format(price);
    }
    public static String priceWithoutDecimal(BigDecimal price) {
        NumberFormat format = DecimalFormat.getInstance();
        format.setRoundingMode(RoundingMode.FLOOR);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(0);
        return format.format(price);
    }

    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        System.out.println(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH")));
        /*double interestRatePerDay = 6.5 / (100.0 * 365.0);
        BigDecimal bigDecimal = BigDecimal.valueOf(interestRatePerDay);
        BigDecimal interest = BigDecimal.valueOf(100000.00).multiply(bigDecimal).setScale(2, BigDecimal.ROUND_CEILING);
        System.out.println(interest);*/
    }
}
