package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.InvestmentInterestEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class GetInvestmentUseCaseImpl implements GetInvestmentUseCase {

    private final InvestmentInterestEntityDao investmentInterestEntityDao;

    @Override
    public InvestmentModel toInvestmentModel(InvestmentEntity investment, AppUserEntity appUser) {

        InvestmentModel model = new InvestmentModel();

        model.setStatus(investment.getInvestmentStatus().name());
        model.setType(investment.getInvestmentType().name());
        model.setAmountInvested(investment.getAmountInvested());
        model.setAccruedInterest(investmentInterestEntityDao.getTotalInterestAmountOnInvestment(investment));
        model.setLockedInvestment(investment.isLockedInvestment());
        model.setInterestRate(investment.getInvestmentTenor().getInterestRate());
        model.setCode(investment.getCode());
        model.setDateWithdrawn(investment.getDateWithdrawn() != null ? investment.getDateWithdrawn().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        model.setMaturityDate(investment.getMaturityDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        model.setDurationInDays(investment.getDurationInDays());
        model.setStartDate(investment.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE));
        model.setTenorName(investment.getInvestmentTenor().getDurationDescription());
        model.setTotalAmountWithdrawn(investment.getTotalAmountWithdrawn());

        model.setAccountId(model.getAccountId());
        model.setCustomerName(appUser.getName());
        model.setUserId(appUser.getUserId());

        return model;
    }
}
