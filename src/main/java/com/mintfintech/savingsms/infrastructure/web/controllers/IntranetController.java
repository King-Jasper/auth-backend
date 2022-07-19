package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.GetCustomerSavingsDataUseCase;
import com.mintfintech.savingsms.usecase.data.response.BusinessLoanResponse;
import com.mintfintech.savingsms.usecase.data.response.CustomerSavingsStatisticResponse;
import com.mintfintech.savingsms.usecase.data.response.HairFinanceLoanResponse;
import com.mintfintech.savingsms.usecase.features.loan.business_loan.CreateBusinessLoanUseCase;
import com.mintfintech.savingsms.usecase.features.loan.business_loan.GetBusinessLoanUseCase;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Sun, 17 Apr, 2022
 */
@Slf4j
@FieldDefaults(makeFinal = true)
@Api(tags = "Intranet Service Endpoints", description = "Services for intra microservice service consumption")
@RestController
@RequestMapping(value = "/api/v1/intranet", headers = {"Authorization", "x-request-client-key"})
@AllArgsConstructor
public class IntranetController {

    private final GetCustomerSavingsDataUseCase getCustomerSavingsDataUseCase;
    private final CreateBusinessLoanUseCase createBusinessLoanUseCase;
    private final GetBusinessLoanUseCase getBusinessLoanUseCase;

    @ApiOperation(value = "Returns a summary funding ")
    @GetMapping(value = "/savings-summary/{accountId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<CustomerSavingsStatisticResponse>> getSavingsSummary(@PathVariable("accountId") String accountId,
                                                                                               @ApiParam(value="Format: dd/MM/yyyy") @DateTimeFormat(pattern="dd/MM/yyyy") @RequestParam(value = "fromDate") LocalDate fromDate) {
        CustomerSavingsStatisticResponse response = getCustomerSavingsDataUseCase.getCustomerSavingsStatistics(accountId);
        ApiResponseJSON<CustomerSavingsStatisticResponse> apiResponseJSON = new ApiResponseJSON<>("Request processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Create a business loan.")
    @PostMapping(value = "/loans/hair-finance/loan-request", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<HairFinanceLoanResponse>> businessLoanRequest(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                        @RequestBody @Valid BusinessLoanRequest request) {

        HairFinanceLoanResponse response = createBusinessLoanUseCase.createHairFinanceLoanRequest(authenticatedUser, request.amount, request.durationInMonths, request.creditAccountId);
        ApiResponseJSON<HairFinanceLoanResponse> apiResponseJSON = new ApiResponseJSON<>("Processed successfully", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a summary funding ")
    @GetMapping(value = "/loans/hair-finance/{loanId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<HairFinanceLoanResponse>> getHairFinanceLoan(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable("loanId") String loanId) {
        /*if(StringUtils.isNotEmpty(clientKey)) {
            System.out.println("clientKey - "+clientKey);
            // Internal MS request does not carry client-key
            //throw new UnauthorisedException("Unauthorised request.");
        }*/
        HairFinanceLoanResponse response = getBusinessLoanUseCase.getHairFinanceLoanDetail(authenticatedUser, loanId);
        ApiResponseJSON<HairFinanceLoanResponse> apiResponseJSON = new ApiResponseJSON<>("Retrieved successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Data
    private static class BusinessLoanRequest {
        @ApiModelProperty(notes = "The loan amount to request. N5000 minimum", required = true)
        @Min(value = 5000, message = "Minimum amount for loan is N5000")
        @NotNull
        private BigDecimal amount;

        private int durationInMonths;

        @ApiModelProperty(notes = "The bank accountId to be credited", required = true)
        @NotEmpty
        private String creditAccountId;
    }
}
