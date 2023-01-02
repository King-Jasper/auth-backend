package com.mintfintech.savingsms.usecase.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by jnwanya on
 * Mon, 02 Jan, 2023
 */
@Data
public class InterestUpdateResponse {
    private double missedAmount;
    private long missedDays;
    private long unappliedDays;
    private List<MissingDate> missingDays;


    @Data
    @AllArgsConstructor
    public static class MissingDate {
        String day;
        double interest;
    }
}
