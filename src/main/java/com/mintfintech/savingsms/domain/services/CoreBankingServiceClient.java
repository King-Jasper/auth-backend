package com.mintfintech.savingsms.domain.services;


import com.mintfintech.savingsms.domain.models.corebankingservice.*;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;

/**
 * Created by jnwanya on
 * Sat, 01 Feb, 2020
 */
public interface CoreBankingServiceClient {
    MsClientResponse<FundTransferResponseCBS> processMintFundTransfer(MintFundTransferRequestCBS transferRequestCBS);
    MsClientResponse<BalanceEnquiryResponseCBS> retrieveAccountBalance(String accountNumber);
    MsClientResponse<FundTransferResponseCBS> processSavingFunding(SavingsFundingRequestCBS transferRequestCBS);
    MsClientResponse<FundTransferResponseCBS> updateAccruedInterest(InterestAccruedUpdateRequestCBS updateRequest);
    MsClientResponse<FundTransferResponseCBS> processSavingsWithdrawal(SavingsWithdrawalRequestCBS requestCBS);
    MsClientResponse<TransactionStatusResponseCBS> reQueryTransactionStatus(TransactionStatusRequestCBS transactionStatusRequestCBS);
}
