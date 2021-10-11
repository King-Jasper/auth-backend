package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.domain.entities.SavingsInterestModel;
import com.mintfintech.savingsms.infrastructure.web.models.*;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.CreateSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.UpdateSavingGoalUseCase;
import com.mintfintech.savingsms.usecase.data.response.AccountSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.RoundUpSavingResponse;
import com.mintfintech.savingsms.usecase.features.roundup_savings.GetRoundUpSavingsUseCase;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import com.mintfintech.savingsms.usecase.models.SavingsTransactionModel;
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
import java.util.List;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@FieldDefaults(makeFinal = true)
@Api(tags = "Savings Goal Management Endpoints",  description = "Handles savings goal transaction management.")
@RestController
@RequestMapping(headers = {"x-request-client-key", "Authorization"})
public class SavingsGoalController {

    private final String v1BaseUrl = "/api/v1/savings-goals";
    private final String v2BaseUrl = "/api/v2/savings-goals";

    private CreateSavingsGoalUseCase createSavingsGoalUseCase;
    private GetSavingsGoalUseCase getSavingsGoalUseCase;
    private UpdateSavingGoalUseCase updateSavingGoalUseCase;
    private GetRoundUpSavingsUseCase getRoundUpSavingsUseCase;
    public SavingsGoalController(CreateSavingsGoalUseCase createSavingsGoalUseCase, GetSavingsGoalUseCase getSavingsGoalUseCase, UpdateSavingGoalUseCase updateSavingGoalUseCase, GetRoundUpSavingsUseCase getRoundUpSavingsUseCase) {
        this.createSavingsGoalUseCase = createSavingsGoalUseCase;
        this.getSavingsGoalUseCase = getSavingsGoalUseCase;
        this.updateSavingGoalUseCase = updateSavingGoalUseCase;
        this.getRoundUpSavingsUseCase = getRoundUpSavingsUseCase;
    }

    @Deprecated
    @ApiOperation(value = "Creates a new savings goal.")
    @PostMapping(value = v1BaseUrl, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalModel>> createSavingsGoalV1(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                               @RequestBody @Valid SavingsGoalCreationRequestJSONV1 goalCreationRequestJSON) {

        SavingsGoalModel response = createSavingsGoalUseCase.createNewSavingsGoal(authenticatedUser, goalCreationRequestJSON.toRequest());
        ApiResponseJSON<SavingsGoalModel> apiResponseJSON = new ApiResponseJSON<>("Savings goal created successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Creates a new savings goal.")
    @PostMapping(value = v2BaseUrl, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalModel>> createSavingsGoal(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                               @RequestBody @Valid SavingsGoalCreationRequestJSON goalCreationRequestJSON) {
        SavingsGoalModel response = createSavingsGoalUseCase.createNewSavingsGoal(authenticatedUser, goalCreationRequestJSON.toRequest());
        ApiResponseJSON<SavingsGoalModel> apiResponseJSON = new ApiResponseJSON<>("Savings goal created successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }



    @ApiOperation(value = "Returns a comprehension list of account savings goal(mint and customer created goals).")
    @GetMapping(value = v1BaseUrl + "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<AccountSavingsGoalResponse>> getAllSavingsGoalList(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        AccountSavingsGoalResponse response = getSavingsGoalUseCase.getAccountSavingsGoals(authenticatedUser);
        RoundUpSavingResponse roundUpSavingResponse = getRoundUpSavingsUseCase.getAccountRoundUpSavings(authenticatedUser);
        response.setRoundUpSaving(roundUpSavingResponse);
        ApiResponseJSON<AccountSavingsGoalResponse> apiResponseJSON = new ApiResponseJSON<>("Savings goals processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of savings goals created by account user.")
    @GetMapping(value = v1BaseUrl, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsGoalModel>>> getAccountSavingsGoalList(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        ApiResponseJSON<List<SavingsGoalModel>> apiResponseJSON = new ApiResponseJSON<>("Savings goals processed successfully.", getSavingsGoalUseCase.getSavingsGoalList(authenticatedUser));
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the details of a savings goal using the goalId.")
    @GetMapping(value = v1BaseUrl + "/{goalId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalModel>> getSavingsGoalDetail(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                         @PathVariable String goalId) {
        ApiResponseJSON<SavingsGoalModel> apiResponseJSON = new ApiResponseJSON<>("Savings goals processed successfully.", getSavingsGoalUseCase.getSavingsGoalByGoalId(authenticatedUser, goalId));
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Updates the saving frequency for a goal.")
    @PutMapping(value = v1BaseUrl + "/{goalId}/saving-frequency", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalModel>> updateSavingsGoalFrequency(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                               @PathVariable String goalId, @RequestBody @Valid SavingsFrequencyUpdateRequestJSON requestJSON) {
        SavingsGoalModel response = updateSavingGoalUseCase.updateSavingFrequency(authenticatedUser, requestJSON.toRequest(goalId));
        ApiResponseJSON<SavingsGoalModel> apiResponseJSON = new ApiResponseJSON<>("Updated successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    /*
    @Deprecated
    @ApiOperation(value = "Change the current saving plan.", notes = "This will extend the savings goal duration.")
    @PutMapping(value = v1BaseUrl + "/{goalId}/change-plan", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsGoalModel>> changeSavingsGoalPlan(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                        @PathVariable String goalId, @RequestBody @Valid SavingsGoalPlanUpdateRequestJSON requestJSON) {
        SavingsGoalModel response = changeSavingsPlanUseCase.changePlan(authenticatedUser, goalId, requestJSON.toRequest());
        ApiResponseJSON<SavingsGoalModel> apiResponseJSON = new ApiResponseJSON<>("Changed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }*/

    @ApiOperation(value = "Returns a list of transactions on savings.")
    @GetMapping(value = v2BaseUrl + "/{goalId}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<SavingsTransactionModel>>> getSavingsTransactions(@PathVariable String goalId,
                                                                                                              @RequestParam(value = "size", defaultValue = "20", required = false) int size,
                                                                                                              @RequestParam(value = "page", defaultValue = "0", required = false) int page) {
        PagedDataResponse<SavingsTransactionModel> response = getSavingsGoalUseCase.getSavingsTransactions(goalId, page, size);
        ApiResponseJSON<PagedDataResponse<SavingsTransactionModel>> apiResponseJSON = new ApiResponseJSON<>("Transactions returned successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of accrued interest on savings.")
    @GetMapping(value = v2BaseUrl + "/{goalId}/accrued-interest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<SavingsInterestModel>>> getSavingsInterest(@PathVariable String goalId, @RequestParam(value = "size", defaultValue = "20", required = false) int size,
                                                                                                       @RequestParam(value = "page", defaultValue = "0", required = false) int page) {
        PagedDataResponse<SavingsInterestModel> response = getSavingsGoalUseCase.getSavingsInterest(goalId, page, size);
        ApiResponseJSON<PagedDataResponse<SavingsInterestModel>> apiResponseJSON = new ApiResponseJSON<>("Accrued interest on savings processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Cancels the saving frequency for a goal (Deactivates auto-save).")
    @DeleteMapping(value = v1BaseUrl + "/{goalId}/saving-frequency", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> cancelsSavingsGoalFrequency(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable String goalId) {
        updateSavingGoalUseCase.cancelSavingFrequency(authenticatedUser, goalId);
        ApiResponseJSON<Object> apiResponseJSON = new ApiResponseJSON<>("Auto-save cancelled successfully.");
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }


}
