package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentWithdrawalStageConstant;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentWithdrawalTypeConstant;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.InvestmentWithdrawalRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.WithdrawalInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import com.mintfintech.savingsms.utils.DateUtil;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.AllArgsConstructor;
import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created by jnwanya on
 * Thu, 20 May, 2021
 */
@Named
@AllArgsConstructor
public class WithdrawalInvestmentUseCaseImpl implements WithdrawalInvestmentUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final InvestmentTenorEntityDao investmentTenorEntityDao;
    private final InvestmentWithdrawalEntityDao investmentWithdrawalEntityDao;
    private final GetInvestmentUseCase getInvestmentUseCase;
    private final ApplicationProperty applicationProperty;

    @Override
    public InvestmentModel liquidateInvestment(AuthenticatedUser authenticatedUser, InvestmentWithdrawalRequest request) {

        InvestmentEntity investment = investmentEntityDao.findByCode(request.getInvestmentCode()).orElseThrow(() -> new BadRequestException("Invalid investment code."));

        if(investment.getInvestmentStatus() != InvestmentStatusConstant.ACTIVE) {
            throw new BadRequestException("Investment is not active. Current status - "+investment.getInvestmentStatus());
        }
        MintAccountEntity account = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        if(!account.getId().equals(investment.getOwner().getId())){
            throw new BusinessLogicConflictException("Sorry, request cannot be processed.");
        }
        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(request.getCreditAccountId(), account)
                .orElseThrow(() -> new BadRequestException("Invalid bank account Id."));

        int minimumLiquidationPeriodInDays = 15;
        if(!applicationProperty.isLiveEnvironment()) {
            minimumLiquidationPeriodInDays = 2;
        }

        long daysPast = investment.getDateCreated().until(LocalDateTime.now(), ChronoUnit.DAYS);
        if(daysPast < minimumLiquidationPeriodInDays) {
            throw new BusinessLogicConflictException("Sorry, your investment has to reach a minimum of "+minimumLiquidationPeriodInDays+" before liquidation.");
        }

        if(request.isFullLiquidation()) {
            processFullLiquidation(investment, creditAccount);
        }else {
            processPartialLiquidation(investment, creditAccount, request.getAmount());
        }
        return getInvestmentUseCase.toInvestmentModel(investment);
    }

    private void processPartialLiquidation(InvestmentEntity investment, MintBankAccountEntity creditAccount, BigDecimal amountToWithdraw) {
         double percentWithdrawal = 70;
         BigDecimal amountInvest = investment.getAmountInvested();
         BigDecimal maximumWithdrawalAmount = amountInvest.subtract(BigDecimal.valueOf(amountInvest.doubleValue() * (percentWithdrawal/ 100.0)));

         if(amountToWithdraw.compareTo(maximumWithdrawalAmount) > 0) {
             String errorMessage = String.format("Maximum amount you can withdraw is %s. That is %f percent of investment amount.",
                     MoneyFormatterUtil.priceWithDecimal(maximumWithdrawalAmount), percentWithdrawal);
             throw new BusinessLogicConflictException(errorMessage);
         }

         InvestmentTenorEntity tenorEntity = investmentTenorEntityDao.getRecordById(investment.getInvestmentTenor().getId());
         double interestPenaltyRate =  tenorEntity.getPenaltyRate();

         BigDecimal accruedInterest = investment.getAccruedInterest();

         BigDecimal interestCharge = BigDecimal.valueOf(accruedInterest.doubleValue() * (interestPenaltyRate/ 100.0));

         // TODO update the withdrawal status to an appropriate value.
        InvestmentWithdrawalEntity withdrawalEntity = InvestmentWithdrawalEntity.builder()
                .amount(amountToWithdraw)
                .amountCharged(interestCharge)
                .balanceBeforeWithdrawal(amountInvest)
                .interestBeforeWithdrawal(accruedInterest)
                .dateForWithdrawal(DateUtil.addWorkingDays(LocalDate.now(), 2))
                .interest(BigDecimal.ZERO)
                .investment(investment)
                .matured(false)
                .withdrawalStage(InvestmentWithdrawalStageConstant.COMPLETED)
                .withdrawalType(InvestmentWithdrawalTypeConstant.PART_PRE_MATURITY_WITHDRAWAL)
                .requestedBy(investment.getCreator())
                .totalAmount(amountToWithdraw)
                .build();
        withdrawalEntity = investmentWithdrawalEntityDao.saveRecord(withdrawalEntity);

        investment.setAmountInvested(amountInvest.subtract(amountToWithdraw));
        investment.setAccruedInterest(accruedInterest.subtract(interestCharge));
        investment.setTotalAmountWithdrawn(investment.getTotalAmountWithdrawn().add(amountToWithdraw));
        investmentEntityDao.saveRecord(investment);
    }

    private void processFullLiquidation(InvestmentEntity investment, MintBankAccountEntity creditAccount) {

        InvestmentTenorEntity tenorEntity = investmentTenorEntityDao.getRecordById(investment.getInvestmentTenor().getId());
        double interestPenaltyRate =  tenorEntity.getPenaltyRate();
        BigDecimal amountInvested = investment.getAmountInvested();
        BigDecimal accruedInterest = investment.getAccruedInterest();

        BigDecimal interestCharge = BigDecimal.valueOf(accruedInterest.doubleValue() * (interestPenaltyRate/ 100.0));
        BigDecimal amountToWithdraw = amountInvested.add(accruedInterest.subtract(interestCharge));

        BigDecimal interestToWithdraw = accruedInterest.subtract(interestCharge);

        // TODO update the withdrawal status to an appropriate value.
        InvestmentWithdrawalEntity withdrawalEntity = InvestmentWithdrawalEntity.builder()
                .amount(amountToWithdraw)
                .amountCharged(interestCharge)
                .balanceBeforeWithdrawal(amountInvested)
                .interestBeforeWithdrawal(accruedInterest)
                .dateForWithdrawal(DateUtil.addWorkingDays(LocalDate.now(), 2))
                .interest(interestToWithdraw)
                .investment(investment)
                .matured(false)
                .withdrawalStage(InvestmentWithdrawalStageConstant.COMPLETED)
                .withdrawalType(InvestmentWithdrawalTypeConstant.FULL_PRE_MATURITY_WITHDRAWAL)
                .requestedBy(investment.getCreator())
                .totalAmount(amountToWithdraw)
                .build();
        withdrawalEntity = investmentWithdrawalEntityDao.saveRecord(withdrawalEntity);

        investment.setAmountInvested(BigDecimal.ZERO);
        investment.setAccruedInterest(BigDecimal.ZERO);
        investment.setInvestmentStatus(InvestmentStatusConstant.LIQUIDATED);
        investment.setTotalInterestWithdrawn(investment.getTotalInterestWithdrawn().add(interestToWithdraw));
        investment.setTotalAmountWithdrawn(investment.getTotalAmountWithdrawn().add(amountToWithdraw));
        investment.setDateWithdrawn(LocalDateTime.now());
        investmentEntityDao.saveRecord(investment);
    }
}
