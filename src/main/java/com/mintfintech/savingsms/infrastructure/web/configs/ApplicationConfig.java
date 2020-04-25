package com.mintfintech.savingsms.infrastructure.web.configs;

import com.mintfintech.savingsms.infrastructure.web.filters.RequestResponseLoggingInterceptor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.util.Collections;

/**
 * Created by jnwanya on
 * Wed, 29 Jan, 2020
 */
@EnableAsync
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
@Configuration
public class ApplicationConfig {

    @Value("${database.url}")
    private String databaseUrl;

    @Value("${database.username}")
    private String databaseUsername;

    @Value("${database.password}")
    private String databasePassword;

    @Value("${database.driver-class-name}")
    private String databaseDriverName;

    @Bean
    public HikariDataSource hikariDataSource() {
        HikariConfig config = new HikariConfig();
        config.addDataSourceProperty("autoReconnect",true);
        config.addDataSourceProperty("maxReconnects",5);
        //config.setMaximumPoolSize(10); // DEFAULT IS 10
        //config.setConnectionTimeout(60000);
        //config.setLeakDetectionThreshold(120000);
        //config.setMaxLifetime(300000);
        config.setPoolName("SavingsMSDBPool");
        config.setUsername(databaseUsername);
        config.setPassword(databasePassword);
        config.setDriverClassName(databaseDriverName);
        config.setJdbcUrl(databaseUrl);
        return new HikariDataSource(config);
    }

   // @LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
       // requestFactory.setBufferRequestBody(false);
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(requestFactory);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(Collections.singletonList(new RequestResponseLoggingInterceptor()));
        return restTemplate;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        String SALT = "fhsjdhk12h3kkslkdsndwe"; //The encryption salt
        return new BCryptPasswordEncoder(12, new SecureRandom(SALT.getBytes()));
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

}
