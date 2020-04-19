package com.mintfintech.savingsms.utils;

import java.time.LocalDateTime;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
public class DateUtil {
    public static boolean sameDay(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
        return firstDateTime.toLocalDate().isEqual(secondDateTime.toLocalDate());
    }
}
