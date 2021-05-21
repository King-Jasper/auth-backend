package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.InvestmentSearchDTO;
import com.mintfintech.savingsms.domain.models.LoanSearchDTO;
import com.mintfintech.savingsms.domain.models.reports.InvestmentStat;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentStatSummary;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import com.mintfintech.savingsms.usecase.models.InvestmentTransactionModel;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private final InvestmentTransactionEntityDao investmentTransactionEntityDao;
    private final InvestmentWithdrawalEntityDao investmentWithdrawalEntityDao;

    @Override
    public List<InvestmentTransactionModel> getInvestmentTransactions(String investmentId) {

        InvestmentEntity investment = investmentEntityDao.findByCode(investmentId)
                .orElseThrow(() -> new NotFoundException("Invalid investment id " + investmentId));

        List<InvestmentTransactionEntity> fundings
                = investmentTransactionEntityDao.getTransactionsByInvestment(investment, TransactionTypeConstant.DEBIT, TransactionStatusConstant.SUCCESSFUL);

        List<InvestmentWithdrawalEntity> withdrawals
                = investmentWithdrawalEntityDao.getWithdrawalByInvestmentAndStatus(investment, InvestmentWithdrawalStatusConstant.PROCESSED);

        List<InvestmentTransactionModel> transactions = new ArrayList<>();

        fundings.forEach(funding -> {
            InvestmentTransactionModel transaction = new InvestmentTransactionModel();
            transaction.setAmount(funding.getTransactionAmount());
            transaction.setDate(funding.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE));
            transaction.setType("Investment Funding");
            transactions.add(transaction);
        });

        withdrawals.forEach(withdrawal -> {
            InvestmentTransactionModel transaction = new InvestmentTransactionModel();
            transaction.setAmount(withdrawal.getAmount());
            transaction.setDate(withdrawal.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE));
            transaction.setType("Investment Liquidation");
            transactions.add(transaction);
        });

        return transactions;
    }

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
       // model.setDurationInDays(investment.getDurationInDays());
        model.setDurationInMonths(investment.getDurationInMonths());
        model.setStartDate(investment.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE));
        model.setTenorName(investment.getInvestmentTenor().getDurationDescription());
        model.setTotalAmountWithdrawn(investment.getTotalAmountWithdrawn());
        model.setTotalExpectedReturn(calculateTotalExpectedReturn(investment));
        model.setPenaltyCharge(investment.getInvestmentTenor().getPenaltyRate());
        model.setMaxLiquidateRate(investment.getMaxLiquidateRate());
        model.setEstimatedProfitAtMaturity(calculateOutstandingInterest(investment).add(investment.getAccruedInterest()));

        //model.setAccountId(model.getAccountId());
        model.setCustomerName(appUser.getName());
        model.setUserId(appUser.getUserId());

        return model;
    }

    @Override
    public InvestmentStatSummary getPagedInvestments(InvestmentSearchRequest searchRequest, int page, int size) {

        Optional<MintAccountEntity> mintAccount = mintAccountEntityDao.findAccountByAccountId(searchRequest.getAccountId());
        InvestmentStatusConstant status = !searchRequest.getInvestmentStatus().equals("ALL") ? InvestmentStatusConstant.valueOf(searchRequest.getInvestmentStatus()) : null;

        InvestmentSearchDTO searchDTO = InvestmentSearchDTO.builder()
                .fromDate(searchRequest.getFromDate() != null ? searchRequest.getFromDate().atStartOfDay() : null)
                .toDate(searchRequest.getToDate() != null ? searchRequest.getToDate().atTime(23, 59) : null)
                .investmentStatus(status)
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
                    if (stat.getInvestmentStatus().equals(InvestmentStatusConstant.COMPLETED)) {
                        summary.setTotalInvestments(stat.getTotalRecords());
                        summary.setTotalInvested(stat.getTotalInvestment());
                        summary.setTotalProfit(stat.getAccruedInterest());
                        summary.setTotalExpectedReturns(stat.getAccruedInterest().add(stat.getTotalInvestment()));
                    } else {
                        summary.setTotalInvestments(stat.getTotalRecords());
                        summary.setTotalInvested(stat.getTotalInvestment());
                        summary.setTotalProfit(stat.getAccruedInterest().add(BigDecimal.valueOf(stat.getOutstandingInterest())));
                        summary.setTotalExpectedReturns(stat.getAccruedInterest().add(BigDecimal.valueOf(stat.getOutstandingInterest())).add(stat.getTotalInvestment()));
                    }
                }
            }
        }
        return summary;
    }

    private BigDecimal calculateOutstandingInterest(InvestmentEntity investment) {

        double interestPerYear = (investment.getInvestmentTenor().getInterestRate() * investment.getAmountInvested().doubleValue()) / 100.0;

        double interestPerMonth = interestPerYear / 12;

        LocalDate maturityDate = investment.getMaturityDate().toLocalDate();
        LocalDate today = LocalDate.now();

        long remainingMonthsToMaturity = ChronoUnit.MONTHS.between(today, maturityDate);

        double outstandingInterest = interestPerMonth * remainingMonthsToMaturity;

        return BigDecimal.valueOf(outstandingInterest);
    }

    private BigDecimal calculateTotalExpectedReturn(InvestmentEntity investment) {

        return investment.getAmountInvested().add(investment.getAccruedInterest()).add(calculateOutstandingInterest(investment));
    }
}
