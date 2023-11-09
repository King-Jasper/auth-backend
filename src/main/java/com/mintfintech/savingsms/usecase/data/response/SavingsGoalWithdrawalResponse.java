package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@NoArgsConstructor
@Getter
@Setter
@Builder
@AllArgsConstructor
public class SavingsGoalWithdrawalResponse {
    private LocalDate withdrawalDate;
    private BigDecimal amountWithdrawal;
    private BigDecimal savingsAmount;
    private BigDecimal interestAmount;
    private BigDecimal withholdingTax;
    private String withdrawalStatus;
    private String savingsName;
    private String goalId;
    private String customerName;
    private String accountNumber;
    private String savingsType;
    private LocalDate startDate;
    private LocalDateTime maturityDate;
    private int duration;
    private double interestRate;

}
