package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Wed, 01 Apr, 2020
 */
@Data
@Builder
public class SavingsGoalCategoryModel {
    private String name;
    private String code;
}
