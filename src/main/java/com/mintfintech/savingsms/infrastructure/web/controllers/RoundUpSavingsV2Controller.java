package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.RoundUpSavingResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Created by jnwanya on
 * Thu, 08 Apr, 2021
 */
@FieldDefaults(makeFinal = true)
@Api(tags = "RoundUp Savings Management Endpoints V2",  description = "Handles roundup savings goal management.")
@RestController
@RequestMapping(headers = {"x-request-client-key", "Authorization"})
public class RoundUpSavingsV2Controller {

    private final String v2BaseUrl = "/api/v3/savings-goals";

    @ApiOperation(value = "Get account roundup savings.")
    @GetMapping(value = v2BaseUrl+ "/roundup-savings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<RoundUpSavingResponse>> getRoundUpSavings(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        RoundUpSavingResponse response = getRoundUpSavingsUseCase.getAccountRoundUpSavings(authenticatedUser);
        ApiResponseJSON<RoundUpSavingResponse> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
