package com.mintfintech.savingsms.domain.services;
import com.mintfintech.savingsms.domain.models.restclient.ClientResponse;

import java.util.Map;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface RestClientService {
    ClientResponse postRequest(String serviceUrl, String requestPayload, Map<String, String> headerMap);
    ClientResponse postRequest(String serviceUrl, String requestPayload);
    ClientResponse getRequest(String serviceUrl, Map<String, String> headerMap);
    ClientResponse getRequest(String serviceUrl);
}
