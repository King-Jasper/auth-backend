package com.mintfintech.savingsms.infrastructure.web.security;

import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;


@Order(1)
@Configuration
public class BasicSecurity extends WebSecurityConfigurerAdapter {

    private final String swaggerUsername;
    private final String swaggerPassword;
    public BasicSecurity(@Value("${swagger-security.username}") String username,
                         @Value("${swagger-security.password}") String password) {
        this.swaggerUsername = username;
        this.swaggerPassword = password;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.csrf().disable();
        http.antMatcher("/v2/api-docs")
                .authorizeRequests().anyRequest().authenticated()
                .and()
                .httpBasic()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        System.out.println("username - "+swaggerUsername+" password - "+swaggerPassword);
        auth.inMemoryAuthentication()
                .withUser(swaggerUsername)
                .password(new BCryptPasswordEncoder().encode(swaggerPassword))
                .roles("ADMIN");
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(){
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("admin realm");
        return entryPoint;
    }
}
