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
import com.mintfintech.savingsms.domain.models.reports.InvestmentStat;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentStatSummary;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetInvestmentUseCaseImpl implements GetInvestmentUseCase {

    private final InvestmentInterestEntityDao investmentInterestEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;

    @Override
    public InvestmentModel toInvestmentModel(InvestmentEntity investment) {

        if (!Hibernate.isInitialized(investment)) {
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
        model.setTotalExpectedReturn(calculateTotalExpectedReturn(investment));
        model.setPenaltyCharge(investment.getInvestmentTenor().getPenaltyRate());
        model.setMaxLiquidateRate(investment.getMaxLiquidateRate());

        //model.setAccountId(model.getAccountId());
        model.setCustomerName(appUser.getName());
        model.setUserId(appUser.getUserId());

        return model;
    }

    @Override
    public InvestmentStatSummary getPagedInvestments(InvestmentSearchRequest searchRequest, int page, int size) {

        Optional<MintAccountEntity> mintAccount = mintAccountEntityDao.findAccountByAccountId(searchRequest.getAccountId());

        InvestmentSearchDTO searchDTO = InvestmentSearchDTO.builder()
                .fromDate(searchRequest.getFromDate() != null ? searchRequest.getFromDate().atStartOfDay() : null)
                .toDate(searchRequest.getToDate() != null ? searchRequest.getToDate().atTime(23, 59) : null)
                .investmentStatus(!searchRequest.getInvestmentStatus().equals("ALL") ? InvestmentStatusConstant.valueOf(searchRequest.getInvestmentStatus()) : null)
                .investmentStatus(status)
                .investmentType(!searchRequest.getInvestmentType().equals("ALL") ? InvestmentTypeConstant.valueOf(searchRequest.getInvestmentType()) : null)
                .account(mintAccount.orElse(null))
                .build();

        Page<InvestmentEntity> investmentEntityPage = investmentEntityDao.searchInvestments(searchDTO, page, size);

        InvestmentStatSummary summary = new InvestmentStatSummary();
        summary.setInvestments(new PagedDataResponse<>(
                investmentEntityPage.getTotalElements(),
                investmentEntityPage.getTotalPages(),
                investmentEntityPage.get().map(this::toInvestmentModel)
                        .collect(Collectors.toList())));

        if (mintAccount.isPresent() && status != null) {

            List<InvestmentStat> stats = investmentEntityDao.getInvestmentStatOnAccount(mintAccount.get());

            for (InvestmentStat stat : stats) {
                if (stat.getInvestmentStatus().equals(status)) {
                    summary.setTotalInvestments(stat.getTotalRecords());
                    summary.setTotalInvested(stat.getTotalInvestment());
                    summary.setTotalProfit(stat.getAccruedInterest().add(BigDecimal.valueOf(stat.getOutstandingInterest())));
                    summary.setTotalExpectedReturns(stat.getAccruedInterest().add(BigDecimal.valueOf(stat.getOutstandingInterest())).add(stat.getTotalInvestment()));
                }

                if (stat.getInvestmentStatus().equals(SavingsGoalStatusConstant.ACTIVE)) {
                    summary.setTotalActiveInvestment(stat.getTotalRecords());
                }
            }

        }
        return summary;
    }

    private BigDecimal calculateTotalExpectedReturn(InvestmentEntity investment) {

        double interestPerYear = (investment.getInvestmentTenor().getInterestRate() * investment.getAmountInvested().doubleValue()) / 100.0;

        double interestPerMonth = interestPerYear / 12;

        LocalDate maturityDate = investment.getMaturityDate().toLocalDate();
        LocalDate today = LocalDate.now();

        long remainingMonthsToMaturity = ChronoUnit.MONTHS.between(today, maturityDate);

        double remainInterest = interestPerMonth * remainingMonthsToMaturity;

        return investment.getAmountInvested().add(investment.getAccruedInterest()).add(BigDecimal.valueOf(remainInterest));
    }
}
