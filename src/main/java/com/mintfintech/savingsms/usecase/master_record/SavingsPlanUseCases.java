package com.mintfintech.savingsms.usecase.master_record;

import com.mintfintech.savingsms.usecase.models.SavingsPlanModel;
import com.mintfintech.savingsms.usecase.models.SavingsPlanTenorModel;
import com.mintfintech.savingsms.usecase.models.deprecated.SavingsPlanDModel;

import java.util.List;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface SavingsPlanUseCases {
    void createDefaultSavingsPlan();
    void createDefaultSavingsTenor();
    List<SavingsPlanDModel> savingsPlanDeprecatedList();
    List<SavingsPlanModel> savingsPlanList();
    List<SavingsPlanTenorModel> savingsTenorList();
}
