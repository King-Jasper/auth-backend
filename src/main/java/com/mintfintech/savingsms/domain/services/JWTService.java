package com.mintfintech.savingsms.domain.services;

import java.util.Map;

/**
 * Created by jnwanya on
 * Wed, 29 Jan, 2020
 */
public interface JWTService {

    String permanent(Map<String, String> attributes, String secretKey);

    String expiringToken(Map<String, String> attributes, String secretKey, int minutes);

    Map<String, String> verify(String token, String secretKey);
}
