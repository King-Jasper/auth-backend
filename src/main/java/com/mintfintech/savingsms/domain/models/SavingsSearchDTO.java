package com.mintfintech.savingsms.domain.models;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Created by jnwanya on
 * Sat, 06 Jun, 2020
 */
@Data
@Builder
public class SavingsSearchDTO {
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private SavingsGoalStatusConstant goalStatus;
    private String goalId;
    private MintAccountEntity account;
    private SavingsPlanEntity savingsPlan;
   // private String customerEmail;
   // private String customerPhoneNumber;
    private boolean autoSavedEnabled;
}
