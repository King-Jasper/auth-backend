package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmergencySavingsModel {
    private boolean exist;
    private List<SavingsGoalModel> savingsGoals;
}
