package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SavingFundingRequest;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
public interface FundSavingsGoalUseCase {
    SavingsGoalFundingResponse fundSavingGoal(MintBankAccountEntity debitAccount, AppUserEntity appUserEntity, SavingsGoalEntity savingsGoal, BigDecimal amount);
    SavingsGoalFundingResponse fundSavingGoal(AuthenticatedUser authenticatedUser, SavingFundingRequest fundingRequest);
    void processSavingsGoalScheduledSaving();
   // SavingsGoalTransactionEntity processFundingTransactionResponse(SavingsGoalTransactionEntity transactionEntity, MsClientResponse<FundTransferResponseCBS> msClientResponse);

}
