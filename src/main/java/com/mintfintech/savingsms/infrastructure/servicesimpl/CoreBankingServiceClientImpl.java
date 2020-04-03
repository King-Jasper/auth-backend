package com.mintfintech.savingsms.infrastructure.servicesimpl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mintfintech.savingsms.domain.models.corebankingservice.BalanceEnquiryResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.MintFundTransferRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.ClientResponse;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
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
        try{
            String requestBody = gson.toJson(transferRequestCBS);
            ClientResponse clientResponse = msRestClientService.postRequest(serviceUrl, requestBody);
            MsClientResponse<FundTransferResponseCBS> response;
            Type collectionType = new TypeToken<MsClientResponse<FundTransferResponseCBS>>(){}.getType();
            response = gson.fromJson(clientResponse.getResponseBody(), collectionType);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(response.getData() != null);
            return response;
        }catch (Exception ex){
            return MsClientResponse.<FundTransferResponseCBS>builder().success(false).build();
        }
    }

    @Override
    public MsClientResponse<BalanceEnquiryResponseCBS> retrieveAccountBalance(String accountNumber) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/account/%s/balance", baseUrl, accountNumber);
        try{
            ClientResponse clientResponse = msRestClientService.getRequest(serviceUrl);
            Type responseType = new TypeToken<MsClientResponse<BalanceEnquiryResponseCBS>>(){}.getType();
            MsClientResponse<BalanceEnquiryResponseCBS> response = gson.fromJson(clientResponse.getResponseBody(), responseType);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(response.getData() != null);
            return response;
        }catch (Exception ex){
            return MsClientResponse.<BalanceEnquiryResponseCBS>builder().success(false).build();
        }
    }

    public String getServiceBaseUrl() {
        // return loadBalancer.choose(applicationProperty.getCoreBankingServiceName()).getUri().toString();
        List<ServiceInstance> list = discoveryClient.getInstances(applicationProperty.getCoreBankingServiceName());
        if (list != null && list.size() > 0 ) {
            return list.get(0).getUri().toString();
        }
        return null;
    }
}
