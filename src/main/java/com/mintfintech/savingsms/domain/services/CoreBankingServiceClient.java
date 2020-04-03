package com.mintfintech.savingsms.domain.services;


import com.mintfintech.savingsms.domain.models.corebankingservice.BalanceEnquiryResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.MintFundTransferRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;

/**
 * Created by jnwanya on
 * Sat, 01 Feb, 2020
 */
public interface CoreBankingServiceClient {
    MsClientResponse<FundTransferResponseCBS> processMintFundTransfer(MintFundTransferRequestCBS transferRequestCBS);
    MsClientResponse<BalanceEnquiryResponseCBS> retrieveAccountBalance(String accountNumber);
}
