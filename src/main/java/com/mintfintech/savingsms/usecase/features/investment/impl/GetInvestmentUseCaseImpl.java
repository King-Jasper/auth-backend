package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentInterestEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.InvestmentSearchDTO;
import com.mintfintech.savingsms.domain.models.LoanSearchDTO;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetInvestmentUseCaseImpl implements GetInvestmentUseCase {

    private final InvestmentInterestEntityDao investmentInterestEntityDao;
    private final InvestmentEntityDao investmentEntityDao;

    @Override
    public InvestmentModel toInvestmentModel(InvestmentEntity investment) {

        if(!Hibernate.isInitialized(investment)) {
            investment = investmentEntityDao.getRecordById(investment.getId());
        }

        InvestmentModel model = new InvestmentModel();

        AppUserEntity appUser = appUserEntityDao.getRecordById(investment.getCreator().getId());

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
        model.setTotalExpectedReturn(BigDecimal.ZERO);

       // model.setAccountId(model.getAccountId());
       // model.setCustomerName(appUser.getName());
       // model.setUserId(appUser.getUserId());

        return model;
    }

    @Override
    public PagedDataResponse<InvestmentModel> getPagedInvestments(InvestmentSearchRequest searchRequest, int page, int size) {

        Optional<MintAccountEntity> mintAccount = mintAccountEntityDao.findAccountByAccountId(searchRequest.getAccountId());

        InvestmentSearchDTO searchDTO = InvestmentSearchDTO.builder()
                .fromDate(searchRequest.getFromDate() != null ? searchRequest.getFromDate().atStartOfDay() : null)
                .toDate(searchRequest.getToDate() != null ? searchRequest.getToDate().atTime(23, 59) : null)
                .investmentStatus(!searchRequest.getInvestmentStatus().equals("ALL") ? SavingsGoalStatusConstant.valueOf(searchRequest.getInvestmentStatus()) : null)
                .investmentType(!searchRequest.getInvestmentType().equals("ALL") ? InvestmentTypeConstant.valueOf(searchRequest.getInvestmentType()) : null)
                .account(mintAccount.orElse(null))
                .build();

        Page<InvestmentEntity> investmentEntityPage = investmentEntityDao.searchInvestments(searchDTO, page, size);

        return new PagedDataResponse<>(investmentEntityPage.getTotalElements(), investmentEntityPage.getTotalPages(),
                investmentEntityPage.get().map(this::toInvestmentModel)
                        .collect(Collectors.toList()));
    }
}
