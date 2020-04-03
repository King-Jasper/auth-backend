package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.SavingFundingRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = "Savings Goal Transaction Endpoints",  description = "Handles savings goal transaction management.")
@RestController
@RequestMapping(value = "/api/v1/savings-goal", headers = {"x-request-client-key", "Authorization"})
public class SavingsGoalTransactionController {

    private FundSavingsGoalUseCase fundSavingsGoalUseCase;
    public SavingsGoalTransactionController(FundSavingsGoalUseCase fundSavingsGoalUseCase) {
        this.fundSavingsGoalUseCase = fundSavingsGoalUseCase;
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
}
