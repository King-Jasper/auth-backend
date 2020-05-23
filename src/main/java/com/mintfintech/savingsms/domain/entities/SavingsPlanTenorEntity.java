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

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SavingsPlanEntity savingsPlan;

    private int duration;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SavingsDurationTypeConstant durationType;

    private double interestRate;

    private String durationDescription;

}
