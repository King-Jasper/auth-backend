package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.models.InvestmentSearchDTO;
import com.mintfintech.savingsms.domain.models.reports.InvestmentStat;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentStatSummary;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import com.mintfintech.savingsms.usecase.models.InvestmentTransactionModel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class GetInvestmentUseCaseImpl implements GetInvestmentUseCase {

    private final InvestmentInterestEntityDao investmentInterestEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final InvestmentTransactionEntityDao investmentTransactionEntityDao;
    private final ApplicationProperty applicationProperty;

    @Override
    public List<InvestmentTransactionModel> getInvestmentTransactions(String investmentId) {

        InvestmentEntity investment = investmentEntityDao.findByCode(investmentId)
                .orElseThrow(() -> new NotFoundException("Invalid investment id " + investmentId));

        List<InvestmentTransactionEntity> transactionEntities = investmentTransactionEntityDao.getTransactionsByInvestment(investment);
        System.out.println("history size - "+transactionEntities.size());
        List<InvestmentTransactionModel> transactions = new ArrayList<>();

        transactionEntities.forEach(funding -> {
            InvestmentTransactionModel transaction = new InvestmentTransactionModel();
            transaction.setAmount(funding.getTransactionAmount());
            transaction.setDate(funding.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE));
            transaction.setType(funding.getTransactionType().name());
            transaction.setTransactionStatus(funding.getTransactionStatus().name());
            transaction.setReference(funding.getTransactionReference());
            if (StringUtils.isNotEmpty(funding.getTransactionDescription())) {
                transaction.setDescription(funding.getTransactionDescription());
            } else {
                if (funding.getTransactionType() == TransactionTypeConstant.DEBIT) {
                    transaction.setDescription("Investment debit.");
                } else {
                    transaction.setDescription("Investment credit.");
                }
            }
            transactions.add(transaction);
        });

        transactions.sort(Comparator.comparing(o -> LocalDate.parse(o.getDate())));

        return transactions;
    }

    @Override
    public InvestmentModel toInvestmentModel(InvestmentEntity investment) {

        if (!Hibernate.isInitialized(investment)) {
            investment = investmentEntityDao.getRecordById(investment.getId());
        }

        InvestmentModel model = new InvestmentModel();

        AppUserEntity appUser = appUserEntityDao.getRecordById(investment.getCreator().getId());

        if (investment.getInvestmentStatus() == InvestmentStatusConstant.COMPLETED || investment.getInvestmentStatus() == InvestmentStatusConstant.LIQUIDATED) {
            model.setAmountInvested(investment.getTotalAmountWithdrawn().subtract(investment.getTotalInterestWithdrawn()));
            model.setTotalAccruedInterest(investment.getTotalInterestWithdrawn());
            model.setTotalExpectedReturn(investment.getTotalAmountWithdrawn());
            model.setEstimatedProfitAtMaturity(investment.getTotalInterestWithdrawn());
        } else {
            BigDecimal totalAccruedInterest = investmentInterestEntityDao.getTotalInterestAmountOnInvestment(investment);
            model.setAmountInvested(investment.getInvestmentStatus() == InvestmentStatusConstant.ACTIVE ? investment.getAmountInvested() : investment.getTotalAmountInvested());
            model.setTotalAccruedInterest(totalAccruedInterest.setScale(2, BigDecimal.ROUND_HALF_UP));
            model.setTotalExpectedReturn(calculateTotalExpectedReturn(investment));
            model.setEstimatedProfitAtMaturity(calculateOutstandingInterest(investment).add(investment.getAccruedInterest()));
        }

        model.setStatus(investment.getInvestmentStatus().name());
        model.setType(investment.getInvestmentType().name());
        model.setAccruedInterest(investment.getAccruedInterest());

        int minimumLiquidationPeriodInDays = applicationProperty.investmentMinimumLiquidationDays();
        if (!applicationProperty.isLiveEnvironment()) {
            minimumLiquidationPeriodInDays = 2;
        }
        boolean canLiquidate = false;
        if (investment.getInvestmentStatus() == InvestmentStatusConstant.ACTIVE) {
            /*long daysPast = investment.getDateCreated().until(LocalDateTime.now(), ChronoUnit.DAYS);
            if (daysPast >= minimumLiquidationPeriodInDays) {
                canLiquidate = true;
            }*/
            long hoursPast = investment.getDateCreated().until(LocalDateTime.now(), ChronoUnit.HOURS);
            if(hoursPast > 12) {
                canLiquidate = true;
            }
        }
        model.setCanLiquidate(canLiquidate);
        model.setInterestRate(investment.getInterestRate());
        model.setCode(investment.getCode());
        model.setDateWithdrawn(investment.getDateWithdrawn() != null ? investment.getDateWithdrawn().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        model.setMaturityDate(investment.getMaturityDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        model.setDurationInMonths(investment.getDurationInMonths());
        model.setStartDate(investment.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE));
        model.setTenorName(investment.getInvestmentTenor().getDurationDescription());
        model.setTotalAmountWithdrawn(investment.getTotalAmountWithdrawn());
        model.setPenaltyCharge(investment.getInvestmentTenor().getPenaltyRate());
        model.setMaxLiquidateRate(investment.getMaxLiquidateRate());
        model.setCustomerName(investment.getOwner().getName());
        model.setUserId(appUser.getUserId());

        return model;
    }

    @Override
    public InvestmentStatSummary getPagedInvestments(InvestmentSearchRequest searchRequest, int page, int size) {

        Optional<MintAccountEntity> mintAccount = mintAccountEntityDao.findAccountByAccountId(searchRequest.getAccountId());
        InvestmentStatusConstant status = null;
        if (StringUtils.isNotEmpty(searchRequest.getInvestmentStatus())) {
            status = InvestmentStatusConstant.valueOf(searchRequest.getInvestmentStatus());
        }

        InvestmentSearchDTO searchDTO = InvestmentSearchDTO.builder()
                .startFromDate(searchRequest.getStartFromDate() != null ? searchRequest.getStartFromDate().atStartOfDay() : null)
                .startToDate(searchRequest.getStartToDate() != null ? searchRequest.getStartToDate().atTime(23, 59) : null)
                .duration(searchRequest.getDuration())
                .customerName(searchRequest.getCustomerName())
                .matureFromDate(searchRequest.getMatureFromDate() != null ? searchRequest.getMatureFromDate().atStartOfDay() : null)
                .matureToDate(searchRequest.getMatureToDate() != null ? searchRequest.getMatureToDate().atTime(23, 59) : null)
                .investmentStatus(status)
                .account(mintAccount.orElse(null))
                .completedRecords(searchRequest.isCompletedRecords())
                .build();

        Page<InvestmentEntity> investmentEntityPage = investmentEntityDao.searchInvestments(searchDTO, page, size);

        InvestmentStatSummary summary = new InvestmentStatSummary();
        summary.setInvestments(new PagedDataResponse<>(
                investmentEntityPage.getTotalElements(),
                investmentEntityPage.getTotalPages(),
                investmentEntityPage.get().map(this::toInvestmentModel)
                        .collect(Collectors.toList())));

        if (mintAccount.isPresent()) {

            if (status != null) {

                if (status == InvestmentStatusConstant.COMPLETED) {
                    List<InvestmentStat> stats = investmentEntityDao.getStatsForCompletedInvestment(mintAccount.get());

                    for (InvestmentStat stat : stats) {
                        if (stat.getInvestmentStatus().equals(InvestmentStatusConstant.LIQUIDATED) || stat.getInvestmentStatus().equals(InvestmentStatusConstant.COMPLETED)) {
                            summary.setTotalInvestments(summary.getTotalInvestments() + stat.getTotalRecords());
                            summary.setTotalInvested(summary.getTotalInvested().add(stat.getTotalInvestment()));
                            summary.setTotalExpectedReturns(summary.getTotalExpectedReturns().add(stat.getTotalExpectedReturns()));
                        }
                    }
                } else {
                    List<InvestmentStat> stats = investmentEntityDao.getInvestmentStatOnAccount(mintAccount.get());

                    for (InvestmentStat stat : stats) {
                        if (stat.getInvestmentStatus().equals(status)) {
                            summary.setTotalInvestments(stat.getTotalRecords());
                            summary.setTotalInvested(stat.getTotalInvestment());
                            summary.setTotalProfit(stat.getAccruedInterest().add(BigDecimal.valueOf(stat.getOutstandingInterest())).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                            summary.setTotalExpectedReturns(summary.getTotalInvested().add(summary.getTotalProfit()).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                        }
                    }
                }
            } else {
                List<InvestmentEntity> investments = investmentEntityDao.getRecordsOnAccount(mintAccount.get());

                long totalRecords = investmentEntityPage.getTotalElements();

                BigDecimal totalInvestment = BigDecimal.ZERO;

                BigDecimal accruedInterest = BigDecimal.ZERO;

                BigDecimal outstandingInterest = BigDecimal.ZERO;

                for (InvestmentEntity investment : investments) {
                    totalInvestment = totalInvestment.add(investment.getAmountInvested());
                    accruedInterest = accruedInterest.add(investment.getAccruedInterest());
                    outstandingInterest = outstandingInterest.add(calculateOutstandingInterest(investment));
                }

                summary.setTotalInvestments(totalRecords);
                summary.setTotalInvested(totalInvestment.setScale(2, BigDecimal.ROUND_HALF_EVEN));
                summary.setTotalProfit(accruedInterest.add(outstandingInterest).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                summary.setTotalExpectedReturns(totalInvestment.add(summary.getTotalProfit()).setScale(2, BigDecimal.ROUND_HALF_EVEN));

            }
        }
        return summary;
    }

    private BigDecimal calculateOutstandingInterest(InvestmentEntity investment) {

        double interestPerAnum = investment.getInterestRate() * 0.01 * investment.getAmountInvested().doubleValue();

        double dailyInterest = interestPerAnum / 365.0;

        LocalDate maturityDate = investment.getMaturityDate().toLocalDate();
        LocalDate today = LocalDate.now();

        long remainingDaysToMaturity = ChronoUnit.DAYS.between(today, maturityDate);

        double outstandingInterest = dailyInterest * remainingDaysToMaturity;

        return BigDecimal.valueOf(outstandingInterest).setScale(2, BigDecimal.ROUND_HALF_EVEN);
    }

    private BigDecimal calculateTotalExpectedReturn(InvestmentEntity investment) {

        BigDecimal totalExpectedReturns = investment.getAmountInvested().add(investment.getAccruedInterest()).add(calculateOutstandingInterest(investment));

        return totalExpectedReturns.setScale(2, BigDecimal.ROUND_HALF_EVEN);
    }
}
