package com.mintfintech.savingsms.infrastructure.web.security;

import com.mintfintech.savingsms.domain.services.JWTService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Wed, 29 Jan, 2020
 */
@Slf4j
@Component
public class JwtTokenProvider extends AbstractUserDetailsAuthenticationProvider {

    private final JWTService jwtService;
    private final ApplicationProperty applicationProperty;
    public JwtTokenProvider(JWTService jwtService, ApplicationProperty applicationProperty) {
        this.jwtService = jwtService;
        this.applicationProperty = applicationProperty;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
       // log.info("additionalAuthenticationChecks called");
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        String token = String.valueOf(authentication.getCredentials());
        return findUserByToken(token).orElseThrow(() -> new UsernameNotFoundException("Invalid authorization token."));
    }

    private Optional<AuthenticatedUser> findUserByToken(String token) {
        Map<String, String> attributes = jwtService.verify(token, applicationProperty.getMicroserviceTokenSecretKey());
        if(attributes.isEmpty()) {
            return Optional.empty();
        }
        if(!attributes.containsKey("userId") || !attributes.containsKey("accountId")) {
            return Optional.empty();
        }
        String userId = attributes.get("userId");
        String accountId = attributes.get("accountId");
        String clientType = attributes.get("client");
       // log.info("accountId: {}, userId: {} client: {}", accountId, userId, clientType);
        String systemGroup = attributes.getOrDefault("system_group", "");
        String privilegeCodes = attributes.getOrDefault("privilege_codes", "");
        String platform = "MOBILE";
        if(clientType.equalsIgnoreCase("web")) {
            platform = "WEB";
        }
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setUserId(userId);
        authenticatedUser.setAccountId(accountId);
        authenticatedUser.setAccessPlatform(platform);
        authenticatedUser.setName(attributes.getOrDefault("name", ""));
        authenticatedUser.setClientType(clientType.toUpperCase());
        authenticatedUser.setPassword(userId);
        if(!systemGroup.isEmpty()) {
            authenticatedUser.addAuthority(systemGroup);
        }
        if(!privilegeCodes.isEmpty()) {
            Arrays.stream(privilegeCodes.split(":")).forEach(authenticatedUser::addAuthority);
        }
        //authenticatedUser.setMsToken(jwtService.expiringToken(stringMap, applicationProperty.getMicroserviceTokenSecretKey(), applicationProperty.getMicroServiceTokenExpiryTimeInMinutes()));
        return Optional.of(authenticatedUser);
    }
}
