package com.mintfintech.savingsms.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
public class DateUtil {
    public static boolean sameDay(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
        return firstDateTime.toLocalDate().isEqual(secondDateTime.toLocalDate());
    }
    public static boolean isAfterDay(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
        return firstDateTime.toLocalDate().isEqual(secondDateTime.toLocalDate());
    }

}
