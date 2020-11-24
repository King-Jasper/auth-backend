package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.*;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.FundWithdrawalUseCase;
import com.mintfintech.savingsms.usecase.data.request.OnlineFundingRequest;
import com.mintfintech.savingsms.usecase.data.response.OnlineFundingResponse;
import com.mintfintech.savingsms.usecase.data.response.ReferenceGenerationResponse;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import com.mintfintech.savingsms.usecase.features.OnlineFundingUseCase;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */

@FieldDefaults(makeFinal = true)
@Api(tags = "Savings Goal Transaction Endpoints",  description = "Handles savings goal transaction management.")
@RestController
@RequestMapping(headers = {"x-request-client-key", "Authorization"})
public class SavingsGoalTransactionController {

    private final String v1BaseUrl = "/api/v1/savings-goals";
    private final String v2BaseUrl = "/api/v2/savings-goals";

    private FundSavingsGoalUseCase fundSavingsGoalUseCase;
    private FundWithdrawalUseCase fundWithdrawalUseCase;
    private OnlineFundingUseCase onlineFundingUseCase;
    public SavingsGoalTransactionController(FundSavingsGoalUseCase fundSavingsGoalUseCase, FundWithdrawalUseCase fundWithdrawalUseCase, OnlineFundingUseCase onlineFundingUseCase) {
        this.fundSavingsGoalUseCase = fundSavingsGoalUseCase;
        this.fundWithdrawalUseCase = fundWithdrawalUseCase;
        this.onlineFundingUseCase = onlineFundingUseCase;
    }

    @ApiOperation(value = "Fund a savings goal.", notes = "Please note that the response code in the return object " +
            "determines if the transaction status. 00: SUCCESSFUL, 02: FAILED, 01: PENDING")
    @PostMapping(value = v1BaseUrl +"/transaction/fund-goal", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalFundingResponse>> fundSavingsGoal(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                       @RequestBody @Valid SavingFundingRequestJSON requestJSON) {

        SavingsGoalFundingResponse response = fundSavingsGoalUseCase.fundSavingGoal(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<SavingsGoalFundingResponse> apiResponseJSON = new ApiResponseJSON<>("Updated successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }


    @Deprecated
    @ApiOperation(value = "Withdraw savings goal fund.", notes = "The amount is needed for goal that has not matured yet, else the saved amount is withdrawn.")
    @PostMapping(value = v1BaseUrl + "/transaction/withdraw-fund", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> withdrawFundFromGoal(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                       @RequestBody @Valid SavingsWithdrawalRequestJSONV1 requestJSON) {
        String message = fundWithdrawalUseCase.withdrawalSavings(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<Object> apiResponseJSON = new ApiResponseJSON<>(message);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Withdraw savings goal fund.")
    @PostMapping(value = v2BaseUrl +"/transaction/withdraw-fund", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> withdrawFundFromGoal(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                        @RequestBody @Valid SavingsWithdrawalRequestJSON requestJSON) {
        String message = fundWithdrawalUseCase.withdrawalSavings(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<Object> apiResponseJSON = new ApiResponseJSON<>(message);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Generate transaction reference for funding savings via the specified payment gateway", notes = "Returns amount in kobo eg N100 is N10000")
    @PostMapping(value = v2BaseUrl +"/transaction/{goalId}/funding-reference", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<ReferenceGenerationResponse>> generateTransactionReference(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                     @PathVariable("goalId") String goalId,
                                                                                                     @RequestBody @Valid OnlineFundingRequestJSON request) {
        OnlineFundingRequest fundingRequest = OnlineFundingRequest.builder()
                .gaolId(goalId).amount(request.getAmount())
                .paymentGateway(request.getPaymentGateway()).build();
        ReferenceGenerationResponse response = onlineFundingUseCase.createFundingRequest(authenticatedUser, fundingRequest);
        response.setAmount(response.getAmount().multiply(BigDecimal.valueOf(100)).setScale(0, BigDecimal.ROUND_DOWN));
        ApiResponseJSON<ReferenceGenerationResponse> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the transaction status of an savings funding request.")
    @GetMapping(value =  v2BaseUrl +"/transaction/reference/{reference}/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<OnlineFundingResponse>> verifyTransactionReference(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                             @PathVariable("reference") String reference) {
        OnlineFundingResponse fundingResponse = onlineFundingUseCase.verifyFundingRequest(authenticatedUser, reference);
        ApiResponseJSON<OnlineFundingResponse> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", fundingResponse);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
