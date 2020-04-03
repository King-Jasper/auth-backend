package com.mintfintech.savingsms.infrastructure.web.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by jnwanya on
 * Thu, 06 Feb, 2020
 */
@Slf4j
public class RequestResponseLoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        logRequest(httpRequest, bytes);
        ClientHttpResponse response = clientHttpRequestExecution.execute(httpRequest, bytes);
        logResponse(response);
        return response;
    }
    private void logRequest(HttpRequest request, byte[] body) throws IOException {
        String requestBody = new String(body, StandardCharsets.UTF_8);
        String url = request.getURI().toString();
        if (log.isDebugEnabled()) {
            log.debug("Method/URI     : {} / {}", request.getMethod(), url);
            log.debug("Request body: {}", requestBody);
        }
    }
    private void logResponse(ClientHttpResponse response) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Status code/Text : {} / {}", response.getStatusCode() , response.getStatusText());
            log.debug("Response body: {}", StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()));
        }
    }
}
