package com.mintfintech.savingsms.usecase.master_record;

import com.mintfintech.savingsms.usecase.models.InvestmentTenorModel;

import java.util.List;

public interface InvestmentPlanUseCase {

    void createDefaultInvestmentTenor();
    List<InvestmentTenorModel> investmentTenorList();
}
