package com.mintfintech.savingsms.domain.services;

/**
 * Created by jnwanya on
 * Wed, 29 Jan, 2020
 */
public interface EnvironmentService {
    String getVariable(String variableName);
    String getVariable(String variableName, String defaultValue);
    String[] getActiveProfiles();
}
