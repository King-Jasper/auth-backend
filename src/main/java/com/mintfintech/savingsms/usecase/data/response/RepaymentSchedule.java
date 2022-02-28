package com.mintfintech.savingsms.usecase.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 28 Feb, 2022
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentSchedule {
    private String repaymentDate;
    private BigDecimal repaymentAmount;
}
