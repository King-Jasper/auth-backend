package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.models.reports.ReportStatisticModel;
import com.mintfintech.savingsms.usecase.GetCustomerSavingsDataUseCase;
import com.mintfintech.savingsms.usecase.data.response.CustomerSavingsStatisticResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import lombok.AllArgsConstructor;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Mon, 18 Apr, 2022
 */
@Named
@AllArgsConstructor
public class GetCustomerSavingsDataUseCaseImpl implements GetCustomerSavingsDataUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private final InvestmentTransactionEntityDao investmentTransactionEntityDao;

    @Override
    public CustomerSavingsStatisticResponse getCustomerSavingsStatistics(String accountId) {
        MintAccountEntity mintAccount = mintAccountEntityDao.findAccountByAccountId(accountId).orElseThrow(() -> new BadRequestException("Invalid account Id"));

        ReportStatisticModel savingsReport = savingsGoalTransactionEntityDao.getSavingsTransactionStatisticsOnAccount(mintAccount);
        ReportStatisticModel investmentReport = investmentTransactionEntityDao.getInvestmentTransactionStatisticsOnAccount(mintAccount);

        return CustomerSavingsStatisticResponse.builder()
                .totalAmountInvested(investmentReport.getTotalAmount())
                .totalInvestment(investmentReport.getTotalRecords())
                .totalSavingsGoal(savingsReport.getTotalRecords())
                .totalAmountSaved(savingsReport.getTotalAmount())
                .build();
    }
}
