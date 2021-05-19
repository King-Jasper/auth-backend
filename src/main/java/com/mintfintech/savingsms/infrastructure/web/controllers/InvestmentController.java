package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.InvestmentCreationRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.features.investment.CreateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentCreationResponseModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;

@FieldDefaults(makeFinal = true)
@Api(tags = "Investment Management Endpoints", description = "Handles investment transaction management.")
@RestController
@RequestMapping(value = "/api/v1/investment/", headers = {"x-request-client-key", "Authorization"})
@RequiredArgsConstructor
@Validated
public class InvestmentController {

    private final CreateInvestmentUseCase createInvestmentUseCase;

    @ApiOperation(value = "Creates a new investment.")
    @PostMapping(value = "create-investment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<InvestmentCreationResponseModel>> createInvestment(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                             @RequestBody @Valid InvestmentCreationRequestJSON requestJSON) {
        InvestmentCreationResponseModel response = createInvestmentUseCase.createInvestment(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<InvestmentCreationResponseModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
