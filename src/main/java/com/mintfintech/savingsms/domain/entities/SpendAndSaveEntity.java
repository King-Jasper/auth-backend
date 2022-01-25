package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.usecase.data.value_objects.RoundUpTransactionCategoryType;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "spend_and_save_setting")
public class SpendAndSaveEntity extends AbstractBaseEntity<Long> {

    @OneToOne(fetch = FetchType.EAGER)
    private MintAccountEntity account;

    @OneToOne(fetch = FetchType.EAGER)
    private AppUserEntity creator;

    @OneToOne(fetch = FetchType.EAGER)
    private SavingsGoalEntity savings;

    private boolean activated;

    private int percentage;

    private boolean isSavingsLocked;

    @Enumerated(value = EnumType.STRING)
    private RoundUpTransactionCategoryType transactionType;

    private LocalDateTime dateActivated;

    private LocalDateTime dateDeactivated;
}
