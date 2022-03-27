package com.mintfintech.savingsms.infrastructure.servicesimpl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mintfintech.savingsms.domain.models.restclient.ClientResponse;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.AffiliateServiceRestClient;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.MsRestClientService;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import javax.inject.Named;
import java.lang.reflect.Type;
import java.util.List;

@Named
public class AffiliateServiceRestClientImpl implements AffiliateServiceRestClient {

    private final MsRestClientService msRestClientService;
    private final ApplicationProperty applicationProperty;
    private final Gson gson;
    private DiscoveryClient discoveryClient;

    @Autowired
    public void setDiscoveryClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public AffiliateServiceRestClientImpl(MsRestClientService msRestClientService, ApplicationProperty applicationProperty, Gson gson) {
        this.msRestClientService = msRestClientService;
        this.applicationProperty = applicationProperty;
        this.gson = gson;
    }
    @Override
    public MsClientResponse<String> validateReferralCode(String code) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("code", code);
        String baseUrl = getServiceBaseUrl();
        String serviceUrl = String.format("%s/api/v1/common/validate-referral", baseUrl);
        try{
            ClientResponse clientResponse = msRestClientService.postRequest(serviceUrl, requestBody.toString());
            MsClientResponse<String> response;
            Type type = new TypeToken<MsClientResponse<String>>(){}.getType();
            response = gson.fromJson(clientResponse.getResponseBody(), type);
            response.setStatusCode(clientResponse.getStatusCode());
            response.setSuccess(true);
            return response;
        }catch (Exception ex){
            return MsClientResponse.<String>builder().success(false).build();
        }
    }

    public String getServiceBaseUrl() {
        if(applicationProperty.isKubernetesEnvironment()) {
            return String.format("http://%s", applicationProperty.getAffiliateServiceName());
        }
        List<ServiceInstance> list = discoveryClient.getInstances(applicationProperty.getAffiliateServiceName());
        if (list != null && list.size() > 0 ) {
            return list.get(0).getUri().toString();
        }
        return null;
    }
}
