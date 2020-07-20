package com.mintfintech.savingsms.infrastructure.web.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Created by jnwanya on
 * Wed, 29 Jan, 2020
 */
@Slf4j
public class JwtTokenFilter extends AbstractAuthenticationProcessingFilter {

    private final static String AUTHORIZATION = "Authorization";

    public JwtTokenFilter(final RequestMatcher protectedRoutes) {
        super(protectedRoutes);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        Optional<String> paramOptional = ofNullable(request.getHeader(AUTHORIZATION));
        if(!paramOptional.isPresent() || !paramOptional.get().toLowerCase().startsWith("bearer")) {
            throw new BadCredentialsException("Bad Authorization Token format.");
        }
        final String token = paramOptional.get().substring(7);
        Authentication authentication = new UsernamePasswordAuthenticationToken(authenticationDetailsSource.buildDetails(request), token);
        return getAuthenticationManager().authenticate(authentication);
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        return true;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);
        chain.doFilter(request, response);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        super.unsuccessfulAuthentication(request, response, failed);
        log.info("authentication failed");
    }
}
