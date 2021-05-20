package com.mintfintech.savingsms.usecase.features.investment;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentStatSummary;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;

public interface GetInvestmentUseCase {

    InvestmentModel toInvestmentModel(InvestmentEntity investment);
    InvestmentStatSummary getPagedInvestments(InvestmentSearchRequest searchRequest, int page, int size);
}
