package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.AccountAuthorisationUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.request.InvestmentCreationRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.features.investment.CreateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.FundInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.data.response.InvestmentCreationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CreateInvestmentUseCaseImpl implements CreateInvestmentUseCase {

    private final AppUserEntityDao appUserEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase;
    private final InvestmentTenorEntityDao investmentTenorEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final GetInvestmentUseCase getInvestmentUseCase;
    private final AccountAuthorisationUseCase accountAuthorisationUseCase;
    private final FundInvestmentUseCase fundInvestmentUseCase;
    private final ApplicationProperty applicationProperty;

    @Override
    @Transactional
    public InvestmentCreationResponse createInvestment(AuthenticatedUser authenticatedUser, InvestmentCreationRequest request) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());

        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(request.getDebitAccountId(), mintAccount)
                .orElseThrow(() -> new BadRequestException("Invalid debit account Id"));

        InvestmentTenorEntity investmentTenor = investmentTenorEntityDao.findById(Long.getLong(request.getTenorId()))
                .orElseThrow(() -> new BadRequestException("Invalid investment tenor Id"));

        if (!mintAccount.getId().equals(debitAccount.getMintAccount().getId())) {
            throw new UnauthorisedException("Request denied.");
        }

        accountAuthorisationUseCase.validationTransactionPin(request.getTransactionPin());

        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);

        BigDecimal investAmount = BigDecimal.valueOf(request.getInvestmentAmount());

        if (debitAccount.getAvailableBalance().compareTo(investAmount) < 0) {
            InvestmentCreationResponse response = new InvestmentCreationResponse();
            response.setInvestment(null);
            response.setCreated(false);
            response.setMessage("Insufficient Funds");
            return response;
        }

        int durationInDays = (investmentTenor.getMaximumDuration() + 1) - investmentTenor.getMinimumDuration();

        InvestmentEntity investment = InvestmentEntity.builder()
                .amountInvested(investAmount)
                .code(investmentEntityDao.generateCode())
                .creator(appUser)
                .investmentStatus(InvestmentStatusConstant.INACTIVE)
                .investmentTenor(investmentTenor)
                .durationInDays(durationInDays)
                .maxLiquidateRate(applicationProperty.getMaxLiquidateRate())
                //.lastInterestApplicationDate(LocalDateTime.now().plusDays(durationInDays - 1))
                .maturityDate(LocalDateTime.now().plusDays(durationInDays))
                .owner(mintAccount)
                .build();

        investment = investmentEntityDao.saveRecord(investment);

        InvestmentTransactionEntity transactionEntity = fundInvestmentUseCase.fundInvestment(investment, debitAccount, investAmount);

        InvestmentCreationResponse response = new InvestmentCreationResponse();
        if(transactionEntity.getTransactionStatus() != TransactionStatusConstant.SUCCESSFUL) {
            investment.setRecordStatus(RecordStatusConstant.DELETED);
            investmentEntityDao.saveRecord(investment);
            response.setInvestment(null);
            response.setCreated(false);
            response.setMessage("Sorry, account debit for investment funding failed.");
            return response;
        }
        investment.setRecordStatus(RecordStatusConstant.ACTIVE);
        investment.setInvestmentStatus(InvestmentStatusConstant.ACTIVE);
        investment.setTotalAmountInvested(investAmount);
        investmentEntityDao.saveRecord(investment);
        response.setInvestment(getInvestmentUseCase.toInvestmentModel(investment));
        response.setCreated(true);
        response.setMessage("Investment Created Successfully");
        return response;
    }

}
