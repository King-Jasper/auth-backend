package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.EmergencySavingsCreationRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.RoundUpSavingResponse;
import com.mintfintech.savingsms.usecase.features.emergency_savings.CreateEmergencySavingsUseCase;
import com.mintfintech.savingsms.usecase.features.emergency_savings.GetEmergencySavingsUseCase;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
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
 * Sun, 01 Nov, 2020
 */
@FieldDefaults(makeFinal = true)
@Api(tags = "Emergency Savings Management Endpoints",  description = "Handles emergency savings goal management.")
@RestController
@RequestMapping(headers = {"x-request-client-key", "Authorization"})
public class EmergencySavingsController {

    private final String v2BaseUrl = "/api/v2/savings-goals";

    private CreateEmergencySavingsUseCase createEmergencySavingsUseCase;
    private GetEmergencySavingsUseCase getEmergencySavingsUseCase;
    public EmergencySavingsController(CreateEmergencySavingsUseCase createEmergencySavingsUseCase, GetEmergencySavingsUseCase getEmergencySavingsUseCase) {
        this.createEmergencySavingsUseCase = createEmergencySavingsUseCase;
        this.getEmergencySavingsUseCase = getEmergencySavingsUseCase;
    }


    @ApiOperation(value = "Creates a new emergency savings goal.")
    @PostMapping(value = v2BaseUrl +"/emergency-savings", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalModel>> createEmergencySavingsGoal(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                               @RequestBody @Valid EmergencySavingsCreationRequestJSON creationRequestJSON) {
        SavingsGoalModel response = createEmergencySavingsUseCase.createSavingsGoal(authenticatedUser, creationRequestJSON.toRequest());
        ApiResponseJSON<SavingsGoalModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Get account emergency savings.")
    @GetMapping(value = v2BaseUrl+ "/emergency-savings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalModel>> getRoundUpSavings(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        SavingsGoalModel response = getEmergencySavingsUseCase.getAccountEmergencySavings(authenticatedUser);
        ApiResponseJSON<SavingsGoalModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
