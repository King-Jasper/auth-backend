package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.Builder;
import lombok.Data;
/**
 * Created by jnwanya on
 * Sat, 31 Oct, 2020
 */
@Builder
@Data
public class RoundUpSavingResponse {
    private boolean exist;
    private Long id;
    private String roundUpType;
    private boolean isActive;
    private SavingsGoalModel savingsGoal;
}
