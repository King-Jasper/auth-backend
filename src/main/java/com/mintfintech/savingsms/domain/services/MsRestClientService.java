package com.mintfintech.savingsms.domain.services;


import com.mintfintech.savingsms.domain.models.restclient.ClientResponse;

/**
 * Created by jnwanya on
 * Sat, 01 Feb, 2020
 */
public interface MsRestClientService {
    ClientResponse postRequest(String serviceUrl, String requestPayload);
    ClientResponse getRequest(String serviceUrl);
}
