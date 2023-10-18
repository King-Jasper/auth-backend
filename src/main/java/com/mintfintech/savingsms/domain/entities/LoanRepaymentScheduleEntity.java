package com.mintfintech.savingsms.domain.entities;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Wed, 27 Sep, 2023
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loan_repayment_schedule_entity")
public class LoanRepaymentScheduleEntity extends AbstractBaseEntity<Long>{

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private LoanRequestEntity loanRequest;

    @Column(nullable = false)
    private LocalDate repaymentDueDate;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Builder.Default
    private BigDecimal principalAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal interestAmount = BigDecimal.ZERO;
}
