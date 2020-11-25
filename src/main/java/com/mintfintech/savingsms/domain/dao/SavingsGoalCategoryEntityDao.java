package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.SavingsGoalCategoryEntity;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Wed, 01 Apr, 2020
 */
public interface SavingsGoalCategoryEntityDao extends CrudDao<SavingsGoalCategoryEntity, Long> {
    Optional<SavingsGoalCategoryEntity> findCategoryByCode(String code);
    SavingsGoalCategoryEntity getCategoryByCode(String code);
    List<SavingsGoalCategoryEntity> getSavingsGoalCategoryList();
    long countSavingsGoalCategory();
}
