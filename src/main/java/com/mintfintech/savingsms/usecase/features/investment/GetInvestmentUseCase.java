package com.mintfintech.savingsms.usecase.features.investment;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;

public interface GetInvestmentUseCase {

    InvestmentModel toInvestmentModel(InvestmentEntity investment);
}
