package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.InvestmentEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentTransactionEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.FundingSourceTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.request.InvestmentFundingRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentFundingResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.investment.FundInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by jnwanya on
 * Wed, 19 May, 2021
 */
@Named
@AllArgsConstructor
public class FundInvestmentUseCaseImpl implements FundInvestmentUseCase {

    private final InvestmentTransactionEntityDao investmentTransactionEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase;
    private final GetInvestmentUseCase getInvestmentUseCase;

    @Override
    public InvestmentTransactionEntity fundInvestment(InvestmentEntity investmentEntity, MintBankAccountEntity debitAccount, BigDecimal amount) {

        String reference = investmentTransactionEntityDao.generateTransactionReference();

        InvestmentTransactionEntity transaction = new InvestmentTransactionEntity();
        transaction.setInvestment(investmentEntity);
        transaction.setBankAccount(debitAccount);
        transaction.setTransactionAmount(amount);
        transaction.setTransactionReference(reference);
        transaction.setTransactionType(TransactionTypeConstant.DEBIT);
        transaction.setTransactionStatus(TransactionStatusConstant.PENDING);
        transaction.setFundingSource(FundingSourceTypeConstant.MINT_ACCOUNT);
        transaction = investmentTransactionEntityDao.saveRecord(transaction);
        processDebit(transaction);
        investmentTransactionEntityDao.saveRecord(transaction);
        return transaction;
    }

    @Override
    public InvestmentFundingResponse fundInvestment(AuthenticatedUser authenticatedUser, InvestmentFundingRequest request) {
        InvestmentEntity investmentEntity = investmentEntityDao.findByCode(request.getInvestmentCode()).orElseThrow(() -> new BadRequestException("Invalid investment code."));
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        if(!investmentEntity.getOwner().getId().equals(accountEntity.getId())) {
            throw new BusinessLogicConflictException("Sorry, request cannot be processed.");
        }
        BigDecimal amount = request.getAmount();
        if(amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("Invalid amount.");
        }
        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(request.getDebitAccountId(), accountEntity)
                .orElseThrow(() -> new BadRequestException("Invalid debit account."));
       debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);
       if(debitAccount.getAvailableBalance().compareTo(amount) < 0) {
           throw new BusinessLogicConflictException("Sorry, you have insufficient balance to fund your investment.");
       }
       if(investmentEntity.getInvestmentStatus() != SavingsGoalStatusConstant.ACTIVE) {
           throw new BusinessLogicConflictException("Sorry is no longer active. Current status - "+investmentEntity.getInvestmentStatus());
       }
       LocalDateTime maturityDate = investmentEntity.getMaturityDate();
       if(LocalDateTime.now().compareTo(maturityDate) >= 0) {
           throw new BusinessLogicConflictException("Sorry, your investment has already matured.");
       }
       InvestmentTransactionEntity transactionEntity = fundInvestment(investmentEntity, debitAccount, amount);
       InvestmentFundingResponse response = new InvestmentFundingResponse();
       String responseCode = "00";
       if(transactionEntity.getTransactionStatus() != TransactionStatusConstant.SUCCESSFUL) {
           responseCode = transactionEntity.getTransactionStatus() == TransactionStatusConstant.PENDING ? "01" : "02";
           response.setResponseCode(responseCode);
           response.setInvestment(getInvestmentUseCase.toInvestmentModel(investmentEntity));
           return response;
       }
       investmentEntity.setAmountInvested(investmentEntity.getAmountInvested().add(amount));
       investmentEntityDao.saveRecord(investmentEntity);
       response.setResponseCode(responseCode);
       response.setInvestment(getInvestmentUseCase.toInvestmentModel(investmentEntity));
       return response;
    }

    private void processDebit(InvestmentTransactionEntity transactionEntity) {
         // process bankone debit
        transactionEntity.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);
        transactionEntity.setExternalReference(RandomStringUtils.random(12));
        transactionEntity.setTransactionResponseMessage("Successful");
        transactionEntity.setTransactionResponseCode("00");
    }

    private String constructInvestmentNarration(InvestmentEntity investment, String reference) {
        String narration = String.format("Mint Investment funding %s %s", investment.getCode(), reference);
        if (narration.length() > 61) {
            return narration.substring(0, 60);
        }
        return narration;
    }
}
