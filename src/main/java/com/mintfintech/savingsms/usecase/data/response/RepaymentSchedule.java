package com.mintfintech.savingsms.usecase.data.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Created by jnwanya on
 * Mon, 28 Feb, 2022
 */
@Data
public class RepaymentSchedule {
    private String repaymentDate;
    @JsonIgnore
    private LocalDate date;
    private BigDecimal repaymentAmount;
    private BigDecimal principal;
    private BigDecimal interest;

    public RepaymentSchedule(LocalDate repaymentDate, BigDecimal principal, BigDecimal interest) {
        this.repaymentDate = repaymentDate.format(DateTimeFormatter.ISO_DATE);
        this.date = repaymentDate;
        this.principal = principal;
        this.interest = interest;
        this.repaymentAmount = principal.add(interest);
    }
}
