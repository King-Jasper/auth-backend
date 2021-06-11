package com.mintfintech.savingsms.infrastructure.servicesimpl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mintfintech.savingsms.domain.models.accountsservice.PinValidationRequest;
import com.mintfintech.savingsms.domain.models.restclient.ClientResponse;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.AccountsRestClient;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.MsRestClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountsRestClientImpl implements AccountsRestClient {

    private final MsRestClientService msRestClientService;
    private final Gson gson;
    private final ApplicationProperty applicationProperty;
    private final DiscoveryClient discoveryClient;

    @Override
    public MsClientResponse<String> validationTransactionPin(PinValidationRequest validationRequest) {
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/intranet/api/v2/validate-pin", baseUrl);
        String requestBody = gson.toJson(validationRequest);
        try {
            ClientResponse clientResponse = msRestClientService.postRequest(serviceUrl, requestBody);
            MsClientResponse<String> response;
            Type type = new TypeToken<MsClientResponse<String>>() {
            }.getType();
            response = gson.fromJson(clientResponse.getResponseBody(), type);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(true);
            return response;
        } catch (Exception ex) {
            return MsClientResponse.<String>builder().success(false).build();
        }
    }

    public String getServiceBaseUrl() {
        if (applicationProperty.isKubernetesEnvironment()) {
            return String.format("http://%s", applicationProperty.getAccountsServiceName());
        }
        List<ServiceInstance> list = discoveryClient.getInstances(applicationProperty.getAccountsServiceName());
        if (list != null && list.size() > 0) {
            return list.get(0).getUri().toString();
        }
        return null;
    }
}
