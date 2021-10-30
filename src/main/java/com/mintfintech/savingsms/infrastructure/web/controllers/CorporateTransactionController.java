package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.ApprovalRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentDetailResponse;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentTopUpDetailResponse;
import com.mintfintech.savingsms.usecase.features.corporate.GetCorporateTransactionUseCase;
import com.mintfintech.savingsms.usecase.features.corporate.ManageTransactionRequestUseCase;
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
@Api(tags = "Corporate Transaction Endpoints", description = "Services for managing corporate transaction")
@RestController
@RequestMapping(value = "/api/v1/corporate/transactions", headers = {"x-request-client-key", "Authorization"})
@AllArgsConstructor
public class CorporateTransactionController {

    private final ManageTransactionRequestUseCase manageTransactionRequestUseCase;
    private final GetCorporateTransactionUseCase getCorporateTransactionUseCase;

    @ApiOperation(value = "Updates transaction request status. APPROVE or DECLINE transaction")
    @PostMapping(value = "/update-status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<String>> updateRequestStatus(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                       @RequestBody ApprovalRequestJSON requestJson) {

        String response = manageTransactionRequestUseCase.processApproval(authenticatedUser, requestJson.toRequest());
        ApiResponseJSON<String> apiResponseJSON = new ApiResponseJSON<>(response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns transaction request.")
    @GetMapping(value = "/{requestId}/investment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<CorporateInvestmentDetailResponse>> fetchCorporateInvestmentDetail(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                              @PathVariable String requestId) {

        CorporateInvestmentDetailResponse response = getCorporateTransactionUseCase.getInvestmentRequestDetail(authenticatedUser, requestId);
        ApiResponseJSON<CorporateInvestmentDetailResponse> apiResponseJSON = new ApiResponseJSON<>("Transactions request processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns transaction request.")
    @GetMapping(value = "/{requestId}/investment-topUp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<CorporateInvestmentTopUpDetailResponse>> fetchCorporateInvestmentTopUpDetail(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                             @PathVariable String requestId) {

        CorporateInvestmentTopUpDetailResponse response = getCorporateTransactionUseCase.getInvestmentTopUpRequestDetail(authenticatedUser, requestId);
        ApiResponseJSON<CorporateInvestmentTopUpDetailResponse> apiResponseJSON = new ApiResponseJSON<>("Transactions request processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

}
