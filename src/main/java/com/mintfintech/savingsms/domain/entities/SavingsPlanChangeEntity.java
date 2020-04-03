package com.mintfintech.savingsms.domain.entities;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "savings_plan_change")
public class SavingsPlanChangeEntity extends AbstractBaseEntity<Long> {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SavingsGoalEntity savingsGoal;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SavingsPlanEntity currentPlan;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SavingsPlanEntity newPlan;

    private BigDecimal savingAmount;

    private BigDecimal accruedInterest;

}
