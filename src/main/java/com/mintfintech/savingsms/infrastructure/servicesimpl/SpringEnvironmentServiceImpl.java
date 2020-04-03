package com.mintfintech.savingsms.infrastructure.servicesimpl;

import com.mintfintech.savingsms.domain.services.EnvironmentService;
import org.springframework.core.env.StandardEnvironment;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Named
public class SpringEnvironmentServiceImpl implements EnvironmentService {

    public StandardEnvironment environment;
    public SpringEnvironmentServiceImpl(StandardEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public String getVariable(String variableName) {
        return environment.getProperty(variableName);
    }

    @Override
    public String getVariable(String variableName, String defaultValue) {
        return environment.getProperty(variableName, defaultValue);
    }

    @Override
    public String[] getActiveProfiles() {
        return environment.getActiveProfiles();
    }
}
