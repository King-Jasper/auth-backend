package com.mintfintech.savingsms.domain.models;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
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
    public enum AutoSaveStatus { ENABLED, DISABLED }
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private SavingsGoalStatusConstant goalStatus;
    private String goalId;
    private MintAccountEntity account;
    private SavingsGoalTypeConstant goalType;
    private String phoneNumber;
    private String customerName;
    private String goalName;
   // private boolean autoSavedEnabled;
    private AutoSaveStatus autoSaveStatus;
}
