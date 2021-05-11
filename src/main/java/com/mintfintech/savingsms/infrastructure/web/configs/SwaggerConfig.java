package com.mintfintech.savingsms.infrastructure.web.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.*;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 06 Feb, 2020
 */
@Profile({"dev", "sandbox", "staging"})
@EnableSwagger2
//@Configuration
public class SwaggerConfig {

    @Autowired
    Optional<GitProperties> gitInfo;


    private BuildProperties buildProperties;

    SwaggerConfig(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Bean
    public Docket api() {
        String version = "1.0";
        if(gitInfo.isPresent()) {
            GitProperties gitProperties = gitInfo.get();
            version = String.format("%s - %s - %s - %s", buildProperties.getVersion(), gitProperties.getShortCommitId(), gitProperties.getBranch(), buildProperties.getTime().toString());
        }

        final List<ResponseMessage> globalResponses = Arrays.asList(
                new ResponseMessageBuilder().code(HttpStatus.OK.value()).message("Request processed successfully").build(),
                new ResponseMessageBuilder().code(HttpStatus.BAD_REQUEST.value()).message("Bad Request, Check request details").build(),
                new ResponseMessageBuilder().code(HttpStatus.UNAUTHORIZED.value()).message("Unauthorised request, invalid credential").build(),
                new ResponseMessageBuilder().code(HttpStatus.NOT_FOUND.value()).message("Requested resource not found.").build(),
                new ResponseMessageBuilder().code(HttpStatus.FORBIDDEN.value()).message("Forbidden access to a resource.").build(),
                new ResponseMessageBuilder().code(HttpStatus.CONFLICT.value()).message("Business Logic Conflict. Error due to unfulfilled business rules").build(),
                new ResponseMessageBuilder().code(HttpStatus.PRECONDITION_FAILED.value()).message("Indicates a condition for fulfilling a request failed " +
                        "even though the request is successful. Eg Login on an unregistered device.").build(),
                new ResponseMessageBuilder().code(HttpStatus.INTERNAL_SERVER_ERROR.value()).message("Oops, Internal Server Error").build()
        );
        return new Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(false)
                .globalResponseMessage(RequestMethod.GET, globalResponses)
                .globalResponseMessage(RequestMethod.POST, globalResponses)
                .globalResponseMessage(RequestMethod.DELETE, globalResponses)
                .globalResponseMessage(RequestMethod.PATCH, globalResponses)
                .globalResponseMessage(RequestMethod.PUT, globalResponses)
                .apiInfo(apiInfo(version))
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.mintfintech.savingsms.infrastructure.web.controllers"))
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.ant("/api/v*/**"))
                .build();

    }
    private ApiInfo apiInfo(String version) {
        return new ApiInfoBuilder()
                .title("Savings Microservice")
                .description("API services for handling customer savings goal and other saving features.")
                .version(version)
                .build();
    }
    @Bean
    public UiConfiguration uiConfig() {
        return new UiConfiguration(true, false, 1, 1, ModelRendering.MODEL, false, DocExpansion.LIST, false, null,
                OperationsSorter.ALPHA, false, TagsSorter.ALPHA, UiConfiguration.Constants.DEFAULT_SUBMIT_METHODS, null);
    }
}
