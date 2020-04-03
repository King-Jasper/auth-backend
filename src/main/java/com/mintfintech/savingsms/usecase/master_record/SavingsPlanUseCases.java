package com.mintfintech.savingsms.usecase.master_record;

import com.mintfintech.savingsms.usecase.models.SavingsPlanModel;

import java.util.List;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface SavingsPlanUseCases {
    void createDefaultSavingsPlan();
    List<SavingsPlanModel> savingsPlanList();
}
