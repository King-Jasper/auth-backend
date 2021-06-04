package com.mintfintech.savingsms.infrastructure.servicesimpl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mintfintech.savingsms.domain.models.corebankingservice.*;
import com.mintfintech.savingsms.domain.models.restclient.ClientResponse;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.MsRestClientService;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import javax.inject.Named;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Named
public class CoreBankingServiceClientImpl implements CoreBankingServiceClient {

    private MsRestClientService msRestClientService;
    private ApplicationProperty applicationProperty;
    private Gson gson;
    private DiscoveryClient discoveryClient;

    public CoreBankingServiceClientImpl(MsRestClientService msRestClientService, ApplicationProperty applicationProperty, Gson gson, DiscoveryClient discoveryClient) {
        this.msRestClientService = msRestClientService;
        this.applicationProperty = applicationProperty;
        this.gson = gson;
        this.discoveryClient = discoveryClient;
    }

    @Override
    public MsClientResponse<FundTransferResponseCBS> processMintFundTransfer(MintFundTransferRequestCBS transferRequestCBS) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/transfer/intra-bank", baseUrl);
        String requestBody = gson.toJson(transferRequestCBS);
        return processTransferRequest(serviceUrl, requestBody);
    }

    @Override
    public MsClientResponse<FundTransferResponseCBS> processSavingFunding(SavingsFundingRequestCBS transferRequestCBS) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/savings-transaction/fund-savings", baseUrl);
        String requestBody = gson.toJson(transferRequestCBS);
        return processTransferRequest(serviceUrl, requestBody);
    }

    @Override
    public MsClientResponse<FundTransferResponseCBS> processSavingReferralFunding(ReferralSavingsFundingRequestCBS fundingRequestCBS) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/savings-transaction/fund-savings/referral", baseUrl);
        String requestBody = gson.toJson(fundingRequestCBS);
        return processTransferRequest(serviceUrl, requestBody);
    }

    @Override
    public MsClientResponse<FundTransferResponseCBS> updateAccruedInterest(InterestAccruedUpdateRequestCBS updateRequest) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/savings-transaction/interest-accrual", baseUrl);
        String requestBody = gson.toJson(updateRequest);
        return processTransferRequest(serviceUrl, requestBody);
    }

    @Override
    public MsClientResponse<FundTransferResponseCBS> processInvestmentFunding(InvestmentFundingRequestCBS requestCBS) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/investment-transaction/fund", baseUrl);
        String requestBody = gson.toJson(requestCBS);
        return processTransferRequest(serviceUrl, requestBody);
    }

    @Override
    public MsClientResponse<FundTransferResponseCBS> processInvestmentWithdrawal(InvestmentWithdrawalRequestCBS requestCBS) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/investment-transaction/withdraw", baseUrl);
        String requestBody = gson.toJson(requestCBS);
        return processTransferRequest(serviceUrl, requestBody);
    }

    @Override
    public MsClientResponse<FundTransferResponseCBS> updateInvestmentAccruedInterest(InterestAccruedUpdateRequestCBS updateRequest) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/investment-transaction/accrual-interest", baseUrl);
        String requestBody = gson.toJson(updateRequest);
        return processTransferRequest(serviceUrl, requestBody);
    }

    @Override
    public MsClientResponse<LoanApplicationResponseCBS> createLoanApplication(LoanApplicationRequestCBS requestCBS) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/loans", baseUrl);

        String requestBody = gson.toJson(requestCBS);
        try {
            ClientResponse clientResponse = msRestClientService.postRequest(serviceUrl, requestBody);
            Type responseType = new TypeToken<MsClientResponse<LoanApplicationResponseCBS>>() {
            }.getType();
            MsClientResponse<LoanApplicationResponseCBS> response = gson.fromJson(clientResponse.getResponseBody(), responseType);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(response.getData() != null);
            return response;
        } catch (Exception ex) {
            return MsClientResponse.<LoanApplicationResponseCBS>builder().success(false).build();
        }
    }

    @Override
    public MsClientResponse<NewLoanAccountResponseCBS> getLoanAccountDetails(String trackingReference) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/loans/new?%s", baseUrl, trackingReference);

        try {
            ClientResponse clientResponse = msRestClientService.getRequest(serviceUrl);
            Type responseType = new TypeToken<MsClientResponse<NewLoanAccountResponseCBS>>() {
            }.getType();
            MsClientResponse<NewLoanAccountResponseCBS> response = gson.fromJson(clientResponse.getResponseBody(), responseType);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(response.getData() != null);
            return response;
        } catch (Exception ex) {
            return MsClientResponse.<NewLoanAccountResponseCBS>builder().success(false).build();
        }
    }

    @Override
    public MsClientResponse<LienAccountResponseCBS> placeLienOnAccount(LienAccountRequestCBS requestCBS) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/account/place-lien", baseUrl);

        String requestBody = gson.toJson(requestCBS);
        try {
            ClientResponse clientResponse = msRestClientService.postRequest(serviceUrl, requestBody);
            Type responseType = new TypeToken<MsClientResponse<LienAccountResponseCBS>>() {
            }.getType();
            MsClientResponse<LienAccountResponseCBS> response = gson.fromJson(clientResponse.getResponseBody(), responseType);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(response.getData() != null);
            return response;
        } catch (Exception ex) {
            return MsClientResponse.<LienAccountResponseCBS>builder().success(false).build();
        }
    }

    @Override
    public MsClientResponse<LienAccountResponseCBS> removeLienOnAccount(LienAccountRequestCBS requestCBS) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/account/remove-lien", baseUrl);

        String requestBody = gson.toJson(requestCBS);
        try {
            ClientResponse clientResponse = msRestClientService.postRequest(serviceUrl, requestBody);
            Type responseType = new TypeToken<MsClientResponse<LienAccountResponseCBS>>() {
            }.getType();
            MsClientResponse<LienAccountResponseCBS> response = gson.fromJson(clientResponse.getResponseBody(), responseType);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(response.getData() != null);
            return response;
        } catch (Exception ex) {
            return MsClientResponse.<LienAccountResponseCBS>builder().success(false).build();
        }
    }

    @Override
    public MsClientResponse<FundTransferResponseCBS> processSavingsWithdrawal(SavingsWithdrawalRequestCBS requestCBS) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/savings-transaction/fund-withdrawal", baseUrl);
        String requestBody = gson.toJson(requestCBS);
        return processTransferRequest(serviceUrl, requestBody);
    }

    @Override
    public MsClientResponse<TransactionStatusResponseCBS> reQueryTransactionStatus(TransactionStatusRequestCBS transactionStatusRequestCBS) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/transfer/requery-transaction-status", baseUrl);
        try {
            String requestBody = gson.toJson(transactionStatusRequestCBS);
            ClientResponse clientResponse = msRestClientService.postRequest(serviceUrl, requestBody);
            MsClientResponse<TransactionStatusResponseCBS> response;
            Type collectionType = new TypeToken<MsClientResponse<TransactionStatusResponseCBS>>() {
            }.getType();
            response = gson.fromJson(clientResponse.getResponseBody(), collectionType);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(response.getData() != null);
            return response;
        } catch (Exception ex) {
            return MsClientResponse.<TransactionStatusResponseCBS>builder().success(false).build();
        }
    }

    @Override
    public MsClientResponse<GeneratedReferenceCBS> generateSavingsFundingReference(SavingsFundingReferenceRequestCBS requestCBS) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/savings-transaction/generate-funding-payment-reference", baseUrl);
        String requestBody = gson.toJson(requestCBS);
        try {
            ClientResponse clientResponse = msRestClientService.postRequest(serviceUrl, requestBody);
            System.out.println(clientResponse.toString());
            MsClientResponse<GeneratedReferenceCBS> response;
            Type collectionType = new TypeToken<MsClientResponse<GeneratedReferenceCBS>>() {
            }.getType();
            response = gson.fromJson(clientResponse.getResponseBody(), collectionType);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(response.getData() != null);
            System.out.println(response.toString());
            return response;
        } catch (Exception ex) {
            return MsClientResponse.<GeneratedReferenceCBS>builder().success(false).build();
        }
    }

    @Override
    public MsClientResponse<SavingsFundingVerificationResponseCBS> verifySavingsFundingRequest(String transactionReference) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/savings-transaction/funding-request/%s/verify", baseUrl, transactionReference);
        try {
            ClientResponse clientResponse = msRestClientService.postRequest(serviceUrl, "");
            MsClientResponse<SavingsFundingVerificationResponseCBS> response;
            Type collectionType = new TypeToken<MsClientResponse<SavingsFundingVerificationResponseCBS>>() {
            }.getType();
            response = gson.fromJson(clientResponse.getResponseBody(), collectionType);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(response.getData() != null);
            return response;
        } catch (Exception ex) {
            return MsClientResponse.<SavingsFundingVerificationResponseCBS>builder().success(false).build();
        }
    }

    private MsClientResponse<FundTransferResponseCBS> processTransferRequest(String serviceUrl, String requestBody) {
        try {
            ClientResponse clientResponse = msRestClientService.postRequest(serviceUrl, requestBody);
            MsClientResponse<FundTransferResponseCBS> response;
            Type collectionType = new TypeToken<MsClientResponse<FundTransferResponseCBS>>() {
            }.getType();
            response = gson.fromJson(clientResponse.getResponseBody(), collectionType);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(response.getData() != null);
            return response;
        } catch (Exception ex) {
            return MsClientResponse.<FundTransferResponseCBS>builder().success(false).build();
        }
    }

    @Override
    public MsClientResponse<BalanceEnquiryResponseCBS> retrieveAccountBalance(String accountNumber) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/account/%s/balance", baseUrl, accountNumber);
        try {
            ClientResponse clientResponse = msRestClientService.getRequest(serviceUrl);
            Type responseType = new TypeToken<MsClientResponse<BalanceEnquiryResponseCBS>>() {
            }.getType();
            MsClientResponse<BalanceEnquiryResponseCBS> response = gson.fromJson(clientResponse.getResponseBody(), responseType);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(response.getData() != null);
            return response;
        } catch (Exception ex) {
            return MsClientResponse.<BalanceEnquiryResponseCBS>builder().success(false).build();
        }
    }

    public String getServiceBaseUrl() {
        if (applicationProperty.isKubernetesEnvironment()) {
            return String.format("http://%s", applicationProperty.getCoreBankingServiceName());
        }
        List<ServiceInstance> list = discoveryClient.getInstances(applicationProperty.getCoreBankingServiceName());
        if (list != null && list.size() > 0) {
            return list.get(0).getUri().toString();
        }
        return null;
    }
}
