package com.mintfintech.savingsms.utils;

import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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

    private static void splitValue() {
       BigDecimal wholeAmount = BigDecimal.valueOf(71044.54);
       BigDecimal baseAmount = BigDecimal.valueOf(50000);
       BigDecimal leftValue = wholeAmount.remainder(baseAmount);
       System.out.println(leftValue);
       System.out.println(wholeAmount.divide(baseAmount, BigDecimal.ROUND_DOWN).intValue());
        System.out.println(leftValue.compareTo(BigDecimal.ZERO) == 0);
    }

    public static void main(String[] args) {

       // BigDecimal temp = BigDecimal.valueOf(158.259999999);
       // System.out.println(temp);
       // temp = BigDecimal.valueOf(158.259999999).setScale(2, BigDecimal.ROUND_HALF_EVEN);
       // System.out.println(temp);
       // LocalDateTime end = LocalDateTime.of(LocalDate.of(2021, 2, 9), LocalTime.of(9, 30));
       // System.out.println(end);
        /*LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateTimeOne = LocalDateTime.parse("2020-04-13T12:32:38.536");
        long daysRemaining = now.until(dateTimeOne, ChronoUnit.DAYS);
        System.out.println("remaining days: "+daysRemaining);
        boolean sameDay = now.toLocalDate().isEqual(dateTimeOne.toLocalDate());
        System.out.println("same Day: "+sameDay);*/
       // System.out.println(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH")));
        /*double interestRatePerDay = 6.5 / (100.0 * 365.0);
        BigDecimal bigDecimal = BigDecimal.valueOf(interestRatePerDay);
        BigDecimal interest = BigDecimal.valueOf(100000.00).multiply(bigDecimal).setScale(2, BigDecimal.ROUND_CEILING);
        System.out.println(interest);*/
        //splitValue();
    }
}
