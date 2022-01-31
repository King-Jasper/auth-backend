package com.mintfintech.savingsms.domain.entities;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;
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

    private LocalDateTime dateActivated;

    private LocalDateTime dateDeactivated;
}
