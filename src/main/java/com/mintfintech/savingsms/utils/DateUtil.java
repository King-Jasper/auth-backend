package com.mintfintech.savingsms.utils;

import java.time.*;
import java.util.Date;

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
    public static LocalDate addWorkingDays(LocalDate date, int workdays) {
        if (workdays < 1) {
            return date;
        }
        LocalDate result = date;
        int addedDays = 0;
        while (addedDays < workdays) {
            result = result.plusDays(1);
            if (!(result.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    result.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                ++addedDays;
            }
        }
        return result;
    }

    public static Date fromLocalDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

}
