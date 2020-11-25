package com.mintfintech.savingsms.usecase.models;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import lombok.Builder;
import lombok.Data;
/**
 * Created by jnwanya on
 * Sun, 08 Nov, 2020
 */
@Builder
@Data
public class EmergencySavingModel {
    private boolean exist;
    private SavingsGoalModel savingsGoal;
}
