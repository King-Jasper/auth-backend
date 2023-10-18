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
    MsClientResponse<FundTransferResponseCBS> processSavingReferralFunding(ReferralSavingsFundingRequestCBS fundingRequestCBS);
    MsClientResponse<FundTransferResponseCBS> updateAccruedInterest(InterestAccruedUpdateRequestCBS updateRequest);
    MsClientResponse<FundTransferResponseCBS> processSavingsWithdrawal(SavingsWithdrawalRequestCBS requestCBS, boolean useV2Endpoint);
  //  MsClientResponse<FundTransferResponseCBS> processSavingsWithdrawalV2(SavingsWithdrawalRequestCBS requestCBS);
    MsClientResponse<TransactionStatusResponseCBS> reQueryTransactionStatus(TransactionStatusRequestCBS transactionStatusRequestCBS);
    MsClientResponse<GeneratedReferenceCBS> generateSavingsFundingReference(SavingsFundingReferenceRequestCBS requestCBS);
    MsClientResponse<SavingsFundingVerificationResponseCBS> verifySavingsFundingRequest(String transactionReference);
    MsClientResponse<FundTransferResponseCBS> processInvestmentFunding(InvestmentFundingRequestCBS requestCBS);
    MsClientResponse<FundTransferResponseCBS> processInvestmentWithdrawal(InvestmentWithdrawalRequestCBS requestCBS);
    MsClientResponse<FundTransferResponseCBS> updateInvestmentAccruedInterest(InterestAccruedUpdateRequestCBS updateRequest);
    MsClientResponse<LoanApplicationResponseCBS> createLoanApplication(LoanApplicationRequestCBS requestCBS);
    MsClientResponse<NewLoanAccountResponseCBS> getLoanAccountDetails(String trackingReference);
    MsClientResponse<LienAccountResponseCBS> placeLienOnAccount(LienAccountRequestCBS requestCBS);
    MsClientResponse<LienAccountResponseCBS> removeLienOnAccount(LienAccountRequestCBS requestCBS);
    MsClientResponse<LoanDetailResponseCBS> getLoanDetails(String customerId, String accountNumber);
}
