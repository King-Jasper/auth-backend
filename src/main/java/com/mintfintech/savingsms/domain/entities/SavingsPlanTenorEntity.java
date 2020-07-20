package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.SavingsDurationTypeConstant;
import lombok.*;

import javax.persistence.*;

/**
 * Created by jnwanya on
 * Tue, 04 Feb, 2020
 */

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "savings_plan_tenor")
public class SavingsPlanTenorEntity extends AbstractBaseEntity<Long> {

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    private SavingsPlanEntity savingsPlan;

    @Builder.Default
    private int duration = 0;

    private int minimumDuration;

    private int maximumDuration;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SavingsDurationTypeConstant durationType;

    private double interestRate;

    private String durationDescription;

}
