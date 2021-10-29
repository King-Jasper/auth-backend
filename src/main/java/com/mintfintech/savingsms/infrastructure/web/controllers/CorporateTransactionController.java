package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.ApprovalRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.CorporateTransactionSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.CorporateTransactionDetailResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.features.corporate.GetCorporateTransactionUseCase;
import com.mintfintech.savingsms.usecase.features.corporate.ManageTransactionRequestUseCase;
import com.mintfintech.savingsms.usecase.models.CorporateTransactionRequestModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.constraints.Pattern;
import java.time.LocalDate;

@FieldDefaults(makeFinal = true)
@Api(tags = "Corporate Transaction Endpoints", description = "Services for managing corporate transaction")
@RestController
@RequestMapping(value = "/api/v1/corporate/transactions", headers = {"x-request-client-key", "Authorization"})
@AllArgsConstructor
public class CorporateTransactionController {

    private final ManageTransactionRequestUseCase manageTransactionRequestUseCase;
    private final GetCorporateTransactionUseCase getCorporateTransactionUseCase;


    @ApiOperation(value = "Returns transaction request.")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<CorporateTransactionRequestModel>>> fetchCorporateTransaction(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                                          @Pattern(regexp = "(ALL|MUTUAL_INVESTMENT|MUTUAL_INVESTMENT_TOPUP)") @RequestParam(value = "transactionType", required = false, defaultValue = "ALL") String transactionType,
                                                                                                                          @Pattern(regexp = "(PENDING|APPROVED|DECLINED)") @RequestParam(value = "approvalStatus", required = false) String approvalStatus,
                                                                                                                          @RequestParam("size") int size, @RequestParam("page") int page,
                                                                                                                          @RequestParam(value = "fromDate", required = false) @DateTimeFormat(pattern="dd/MM/yyyy") LocalDate fromDate,
                                                                                                                          @RequestParam(value = "toDate", required = false) @DateTimeFormat(pattern="dd/MM/yyyy")  LocalDate toDate) {
        if("ALL".equalsIgnoreCase(transactionType)) {
            transactionType = "";
        }
        CorporateTransactionSearchRequest request = CorporateTransactionSearchRequest.builder()
                .approvalStatus(approvalStatus)
                .transactionType(transactionType)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
        PagedDataResponse<CorporateTransactionRequestModel> response = getCorporateTransactionUseCase.getTransactionRequest(authenticatedUser, request, page, size);
        ApiResponseJSON<PagedDataResponse<CorporateTransactionRequestModel>> apiResponseJSON = new ApiResponseJSON<>("Transactions request processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Updates transaction request status. APPROVE or DECLINE transaction")
    @PostMapping(value = "/update-status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<String>> updateRequestStatus(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                       @RequestBody ApprovalRequestJSON requestJson) {

        String response = manageTransactionRequestUseCase.processApproval(authenticatedUser, requestJson.toRequest());
        ApiResponseJSON<String> apiResponseJSON = new ApiResponseJSON<>(response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns transaction request.")
    @GetMapping(value = "/{requestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<CorporateTransactionDetailResponse>> fetchCorporateTransactionDetail(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                               @PathVariable String requestId) {

        CorporateTransactionDetailResponse response = getCorporateTransactionUseCase.getTransactionRequestDetail(authenticatedUser, requestId);
        ApiResponseJSON<CorporateTransactionDetailResponse> apiResponseJSON = new ApiResponseJSON<>("Transactions request processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

}
