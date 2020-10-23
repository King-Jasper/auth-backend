package com.mintfintech.savingsms.domain.entities;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 23 Oct, 2020
 */
@Builder
@Data
public class SavingsInterestModel {

    private BigDecimal interestAmount;

    private double rate;

    private BigDecimal savingsBalance;
}
