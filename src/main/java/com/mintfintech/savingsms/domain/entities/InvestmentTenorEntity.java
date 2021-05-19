package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.SavingsDurationTypeConstant;
import lombok.*;
import javax.persistence.*;
/**
 * Created by jnwanya on
 * Mon, 17 May, 2021
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "investment_tenor")
public class InvestmentTenorEntity extends AbstractBaseEntity<Long> {

    private int minimumDuration;

    private int maximumDuration;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SavingsDurationTypeConstant durationType;

    private double interestRate;

    private String durationDescription;

    private double penaltyRate;
}
