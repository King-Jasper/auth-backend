package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.SavingsPlanTypeConstant;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "savings_plan")
public class SavingsPlanEntity extends AbstractBaseEntity<Long> {

    @Column(nullable = false, unique = true)
    private String planId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SavingsPlanTypeConstant planName;

    private String description;

    @Column(nullable = false)
    private BigDecimal minimumBalance;

    @Column(nullable = false)
    private BigDecimal maximumBalance;

    private double interestRate;

}
