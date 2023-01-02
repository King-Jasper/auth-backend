package com.mintfintech.savingsms.usecase.data.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 02 Jan, 2023
 */
@Data
public class InterestUpdateResponse {
    private double missedAmount;
    private long missedDays;
    private long unappliedDays;
}
