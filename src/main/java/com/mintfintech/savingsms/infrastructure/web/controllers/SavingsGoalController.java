package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.SavingsFrequencyUpdateRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.models.SavingsGoalCreationRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.models.SavingsGoalPlanUpdateRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.ChangeSavingsPlanUseCase;
import com.mintfintech.savingsms.usecase.CreateSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.UpdateSavingGoalUseCase;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Api(tags = "Savings Goal Management Endpoints",  description = "Handles savings goal transaction management.")
@RestController
@RequestMapping(value = "/api/v1/savings-goal", headers = {"x-request-client-key", "Authorization"})
public class SavingsGoalController {

    private CreateSavingsGoalUseCase createSavingsGoalUseCase;
    private GetSavingsGoalUseCase getSavingsGoalUseCase;
    private UpdateSavingGoalUseCase updateSavingGoalUseCase;
    private ChangeSavingsPlanUseCase changeSavingsPlanUseCase;
    public SavingsGoalController(CreateSavingsGoalUseCase createSavingsGoalUseCase, GetSavingsGoalUseCase getSavingsGoalUseCase, UpdateSavingGoalUseCase updateSavingGoalUseCase, ChangeSavingsPlanUseCase changeSavingsPlanUseCase) {
        this.createSavingsGoalUseCase = createSavingsGoalUseCase;
        this.getSavingsGoalUseCase = getSavingsGoalUseCase;
        this.updateSavingGoalUseCase = updateSavingGoalUseCase;
        this.changeSavingsPlanUseCase = changeSavingsPlanUseCase;
    }

    @ApiOperation(value = "Creates a new savings goal.")
    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalModel>> createSavingsGoal(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                               @RequestBody @Valid SavingsGoalCreationRequestJSON goalCreationRequestJSON) {
        SavingsGoalModel response = createSavingsGoalUseCase.createNewSavingsGoal(authenticatedUser, goalCreationRequestJSON.toRequest());
        ApiResponseJSON<SavingsGoalModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of savings goals for an account.")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsGoalModel>>> getAccountSavingsGoalList(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        ApiResponseJSON<List<SavingsGoalModel>> apiResponseJSON = new ApiResponseJSON<>("Retrieved successfully.", getSavingsGoalUseCase.getSavingsGoalList(authenticatedUser));
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of savings goals for an account.")
    @GetMapping(value = "/{goalId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalModel>> getSavingsGoalDetail(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                         @PathVariable String goalId) {
        ApiResponseJSON<SavingsGoalModel> apiResponseJSON = new ApiResponseJSON<>("Retrieved successfully.", getSavingsGoalUseCase.getSavingsGoalByGoalId(authenticatedUser, goalId));
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Updates the saving frequency for a goal.")
    @PutMapping(value = "/{goalId}/saving-frequency", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalModel>> updateSavingsGoalFrequency(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                               @PathVariable String goalId, @RequestBody @Valid SavingsFrequencyUpdateRequestJSON requestJSON) {
        SavingsGoalModel response = updateSavingGoalUseCase.updateSavingFrequency(authenticatedUser, requestJSON.toRequest(goalId));
        ApiResponseJSON<SavingsGoalModel> apiResponseJSON = new ApiResponseJSON<>("Updated successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Change the current saving plan.")
    @PutMapping(value = "/{goalId}/change-plan", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalModel>> changeSavingsGoalPlan(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                        @PathVariable String goalId, @RequestBody @Valid SavingsGoalPlanUpdateRequestJSON requestJSON) {
        SavingsGoalModel response = changeSavingsPlanUseCase.changePlan(authenticatedUser, goalId, requestJSON.getPlanId());
        ApiResponseJSON<SavingsGoalModel> apiResponseJSON = new ApiResponseJSON<>("Changed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Cancels the saving frequency for a goal (Deactivates auto-save).")
    @DeleteMapping(value = "/{goalId}/saving-frequency", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> cancelsSavingsGoalFrequency(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable String goalId) {
        updateSavingGoalUseCase.cancelSavingFrequency(authenticatedUser, goalId);
        ApiResponseJSON<Object> apiResponseJSON = new ApiResponseJSON<>("Auto-save cancelled successfully.");
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
