package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.OnlineFundingRequest;
import com.mintfintech.savingsms.usecase.data.request.SavingFundingRequest;
import com.mintfintech.savingsms.usecase.data.response.OnlineFundingResponse;
import com.mintfintech.savingsms.usecase.data.response.ReferenceGenerationResponse;
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
    String constructFundingNarration(SavingsGoalEntity savingsGoalEntity);
    SavingsGoalTransactionEntity processFundingTransactionResponse(SavingsGoalTransactionEntity transactionEntity, MsClientResponse<FundTransferResponseCBS> msClientResponse);

}
