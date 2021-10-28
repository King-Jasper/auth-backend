package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.ApprovalRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.features.investment.CorporateInvestmentUseCase;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@FieldDefaults(makeFinal = true)
@Api(tags = "Corporate Investment Endpoints", description = "Services for managing corporate investments")
@RestController
@RequestMapping(value = "/api/v1/corporate/investments", headers = {"x-request-client-key", "Authorization"})
@AllArgsConstructor
public class CorporateInvestmentController {

    private final CorporateInvestmentUseCase corporateInvestmentUseCase;

    @ApiOperation(value = "Updates transaction request status. APPROVE or DECLINE transaction")
    @PostMapping(value = "/update-status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<String>> updateRequestStatus(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                       @RequestBody ApprovalRequestJSON requestJson) {

        String response = corporateInvestmentUseCase.processApproval(authenticatedUser, requestJson.toRequest());
        ApiResponseJSON<String> apiResponseJSON = new ApiResponseJSON<>(response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

}
