package com.mintfintech.savingsms.infrastructure.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.core.GrantedAuthorityDefaults;

/**
 * Created by jnwanya on
 * Sat, 02 May, 2020
 */
@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true)
public class CustomGlobalMethodSecurity extends GlobalMethodSecurityConfiguration {
    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults("");
    }
}
