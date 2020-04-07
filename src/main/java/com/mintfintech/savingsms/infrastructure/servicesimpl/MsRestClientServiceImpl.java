package com.mintfintech.savingsms.infrastructure.servicesimpl;

import com.mintfintech.savingsms.domain.models.restclient.ClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.JWTService;
import com.mintfintech.savingsms.domain.services.MsRestClientService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Slf4j
@Named
public class MsRestClientServiceImpl implements MsRestClientService {

    private ApplicationProperty applicationProperty;
    private JWTService jwtService;
    private RestTemplate restTemplate;
    public MsRestClientServiceImpl(ApplicationProperty applicationProperty, JWTService jwtService, RestTemplate restTemplate) {
        this.applicationProperty = applicationProperty;
        this.jwtService = jwtService;
        this.restTemplate = restTemplate;
    }

    @Override
    public ClientResponse postRequest(String serviceUrl, String requestPayload) {
        ResponseEntity<String> responseEntity = postRequest(requestPayload, serviceUrl, createRequestHeader());
        return ClientResponse.builder()
                .statusCode(responseEntity.getStatusCodeValue())
                .responseBody(StringUtils.defaultString(responseEntity.getBody()))
                .build();
    }

    @Override
    public ClientResponse getRequest(String serviceUrl) {
        ResponseEntity<String> responseEntity = getRequest(serviceUrl, createRequestHeader());
        return ClientResponse.builder()
                .statusCode(responseEntity.getStatusCodeValue())
                .responseBody(StringUtils.defaultString(responseEntity.getBody()))
                .build();
    }

    private ResponseEntity<String> postRequest(String requestPayload, String serviceUrl, HttpHeaders headers) {
        HttpEntity<String> entity = new HttpEntity<>(requestPayload, headers);
        try{
            return restTemplate.exchange(serviceUrl, HttpMethod.POST, entity, String.class);
        }catch (HttpStatusCodeException e){
            return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (Exception exception){
            log.error("Exception: {}", ExceptionUtils.getStackTrace(exception));
            return new ResponseEntity<>(HttpStatus.GATEWAY_TIMEOUT);
        }
    }
    private ResponseEntity<String> getRequest(String serviceUrl, HttpHeaders headers) {
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try{
            return restTemplate.exchange(serviceUrl, HttpMethod.GET, entity, String.class);
        }catch (HttpStatusCodeException e){
            return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (Exception exception){
            log.error("Exception: {}", ExceptionUtils.getStackTrace(exception));
            return new ResponseEntity<>(HttpStatus.GATEWAY_TIMEOUT);
        }
    }

    private HttpHeaders createRequestHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer "+generateRequestToken());
        headers.set("x-request-client-key", RandomStringUtils.randomAlphabetic(10));
        return headers;
    }

    private String generateRequestToken() {
        String userId;
        String accountId;
        String clientType = "";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null && authentication.getPrincipal() != null) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
            userId = authenticatedUser.getUserId();
            accountId = authenticatedUser.getAccountId();
            clientType = authenticatedUser.getClientType();
        }else {
            userId = RandomStringUtils.randomAlphanumeric(8);
            accountId = userId;
        }
        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("userId", userId);
        stringMap.put("accountId", accountId);
        stringMap.put("client", clientType);

        return jwtService.expiringToken(stringMap, applicationProperty.getMicroserviceTokenSecretKey(), applicationProperty.getMicroServiceTokenExpiryTimeInMinutes());
    }

}
