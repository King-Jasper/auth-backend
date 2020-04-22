package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.SavingFundingRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.models.SavingsWithdrawalRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.FundWithdrawalUseCase;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
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

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@FieldDefaults(makeFinal = true)
@Api(tags = "Savings Goal Transaction Endpoints",  description = "Handles savings goal transaction management.")
@RestController
@RequestMapping(value = "/api/v1/savings-goals", headers = {"x-request-client-key", "Authorization"})
public class SavingsGoalTransactionController {

    private FundSavingsGoalUseCase fundSavingsGoalUseCase;
    private FundWithdrawalUseCase fundWithdrawalUseCase;
    public SavingsGoalTransactionController(FundSavingsGoalUseCase fundSavingsGoalUseCase, FundWithdrawalUseCase fundWithdrawalUseCase) {
        this.fundSavingsGoalUseCase = fundSavingsGoalUseCase;
        this.fundWithdrawalUseCase = fundWithdrawalUseCase;
    }

    @ApiOperation(value = "Fund a savings goal.", notes = "Please note that the response code in the return object " +
            "determines if the transaction status. 00: SUCCESSFUL, 02: FAILED, 01: PENDING")
    @PostMapping(value = "/transaction/fund-goal", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalFundingResponse>> fundSavingsGoal(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                       @RequestBody @Valid SavingFundingRequestJSON requestJSON) {

        SavingsGoalFundingResponse response = fundSavingsGoalUseCase.fundSavingGoal(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<SavingsGoalFundingResponse> apiResponseJSON = new ApiResponseJSON<>("Updated successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Withdraw from a savings goal.", notes = "The amount is needed for goal that has not matured yet, else the saved amount is withdrawn.")
    @PostMapping(value = "/transaction/withdraw-fund", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> withdrawFundFromGoal(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                       @RequestBody @Valid SavingsWithdrawalRequestJSON requestJSON) {
        String message = fundWithdrawalUseCase.withdrawalSavings(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<Object> apiResponseJSON = new ApiResponseJSON<>(message);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
