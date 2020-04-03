package com.mintfintech.savingsms.usecase.master_record;

import com.mintfintech.savingsms.usecase.models.SavingsGoalCategoryModel;

import java.util.List;

/**
 * Created by jnwanya on
 * Wed, 01 Apr, 2020
 */
public interface SavingsGoalCategoryUseCase {
    void createDefaultSavingsCategory();
    List<SavingsGoalCategoryModel> savingsGoalCategoryList();
}
