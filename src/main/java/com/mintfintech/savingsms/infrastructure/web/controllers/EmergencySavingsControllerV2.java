package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.EmergencySavingsCreationRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.models.SavingsWithdrawalRequestJSONV2;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.features.emergency_savings.CreateEmergencySavingsUseCase;
import com.mintfintech.savingsms.usecase.features.emergency_savings.GetEmergencySavingsUseCase;
import com.mintfintech.savingsms.usecase.features.emergency_savings.WithdrawEmergencySavingsUseCase;
import com.mintfintech.savingsms.usecase.models.EmergencySavingModel;
import com.mintfintech.savingsms.usecase.models.EmergencySavingsModel;
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

@FieldDefaults(makeFinal = true)
@Api(tags = "Emergency Savings Management Endpoints",  description = "Handles emergency savings goal management.")
@RestController
@RequestMapping(headers = {"x-request-client-key", "Authorization"})
public class EmergencySavingsControllerV2 {

    private final String v3BaseUrl = "/api/v3/savings-goals";

    private CreateEmergencySavingsUseCase createEmergencySavingsUseCase;
    private GetEmergencySavingsUseCase getEmergencySavingsUseCase;
    private WithdrawEmergencySavingsUseCase withdrawEmergencySavingsUseCase;
    public EmergencySavingsControllerV2(CreateEmergencySavingsUseCase createEmergencySavingsUseCase, GetEmergencySavingsUseCase getEmergencySavingsUseCase, WithdrawEmergencySavingsUseCase withdrawEmergencySavingsUseCase) {
        this.createEmergencySavingsUseCase = createEmergencySavingsUseCase;
        this.getEmergencySavingsUseCase = getEmergencySavingsUseCase;
        this.withdrawEmergencySavingsUseCase = withdrawEmergencySavingsUseCase;
    }

    @ApiOperation(value = "Creates a new emergency savings goal.")
    @PostMapping(value = v3BaseUrl +"/emergency-savings", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<EmergencySavingsModel>> createEmergencySavingsGoalV2(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                               @RequestBody @Valid EmergencySavingsCreationRequestJSON creationRequestJSON) {
        EmergencySavingsModel response = createEmergencySavingsUseCase.createSavingsGoalV2(authenticatedUser, creationRequestJSON.toRequest());
        ApiResponseJSON<EmergencySavingsModel> apiResponseJSON = new ApiResponseJSON<>("Emergency saving goals created successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Get account emergency savings.")
    @GetMapping(value = v3BaseUrl+ "/emergency-savings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<EmergencySavingsModel>> getEmergencySavings(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        EmergencySavingsModel response = getEmergencySavingsUseCase.getAccountEmergencySavingsV2(authenticatedUser);
        ApiResponseJSON<EmergencySavingsModel> apiResponseJSON = new ApiResponseJSON<>("Emergency savings returned successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Withdraw savings goal fund.")
    @PostMapping(value = v3BaseUrl +"/transaction/withdraw-fund", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> withdrawFundFromGoalV2(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                          @RequestBody @Valid SavingsWithdrawalRequestJSONV2 requestJSON) {
        String message = withdrawEmergencySavingsUseCase.withdrawalSavingsV2(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<Object> apiResponseJSON = new ApiResponseJSON<>(message);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Get account emergency saving.")
    @GetMapping(value = v3BaseUrl+ "/emergency-savings/{goal-id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<EmergencySavingModel>> getEmergencySaving(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                    @PathVariable("goal-id") String goalId) {
        EmergencySavingModel response = getEmergencySavingsUseCase.getEmergencySaving(authenticatedUser, goalId);
        ApiResponseJSON<EmergencySavingModel> apiResponseJSON = new ApiResponseJSON<>("Emergency savings returned successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
