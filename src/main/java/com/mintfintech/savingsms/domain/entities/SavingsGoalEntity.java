package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.SavingsFrequencyTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
@Table(name = "savings_goal")
public class SavingsGoalEntity extends AbstractBaseEntity<Long> {

    @Column(nullable = false, unique = true)
    private String goalId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MintAccountEntity mintAccount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUserEntity creator;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SavingsPlanEntity savingsPlan;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private SavingsPlanTenorEntity savingsPlanTenor;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    private SavingsGoalCategoryEntity goalCategory;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SavingsGoalCreationSourceConstant creationSource;

    @Enumerated(EnumType.STRING)
    private SavingsGoalStatusConstant goalStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SavingsGoalTypeConstant savingsGoalType;

    private String name;

    @Builder.Default
    private BigDecimal savingsBalance = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal savingsAmount = BigDecimal.ZERO; // the frequency amount to be saved.

    @Builder.Default
    private BigDecimal targetAmount = BigDecimal.ZERO;

    @Builder.Default
    private boolean autoSave = false;

    @Builder.Default
    private boolean lockedSavings = true;

    @Enumerated(EnumType.STRING)
    private SavingsFrequencyTypeConstant savingsFrequency;

    private LocalDateTime nextAutoSaveDate;

    private LocalDateTime maturityDate;

    private LocalDateTime lastInterestApplicationDate;

    private LocalDate savingsStartDate;

    @Builder.Default
    private int selectedDuration = 0;

}
