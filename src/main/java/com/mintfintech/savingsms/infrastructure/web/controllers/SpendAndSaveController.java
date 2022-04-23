package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.google.gson.Gson;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.EditSpendAndSaveRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SpendAndSaveSetUpRequest;
import com.mintfintech.savingsms.usecase.data.request.SpendAndSaveWithdrawalRequest;
import com.mintfintech.savingsms.usecase.data.response.SpendAndSaveResponse;
import com.mintfintech.savingsms.usecase.features.spend_and_save.CreateSpendAndSaveUseCase;
import com.mintfintech.savingsms.usecase.features.spend_and_save.GetSpendAndSaveUseCase;
import com.mintfintech.savingsms.usecase.features.spend_and_save.UpdateSpendAndSaveUseCase;
import com.mintfintech.savingsms.usecase.features.spend_and_save.WithdrawSpendAndSaveUseCase;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@FieldDefaults(makeFinal = true)
@RestController
@AllArgsConstructor
@Api(tags = "Spend And Save Management Endpoints",  description = "Handles spend and save management.")
@RequestMapping(value = "api/v1/savings-goals/spend-and-save", headers = {"x-request-client-key", "Authorization"})
public class SpendAndSaveController {

    private final CreateSpendAndSaveUseCase createSpendAndSaveUseCase;
    private final UpdateSpendAndSaveUseCase updateSpendAndSaveUseCase;
    private final GetSpendAndSaveUseCase getSpendAndSaveUseCase;
    private final WithdrawSpendAndSaveUseCase withdrawSpendAndSaveUseCase;
    private final SystemIssueLogService systemIssueLogService;
    private final Gson gson;

    @ApiOperation("Handles the set up of spend and save")
    @PostMapping(value = "/set-up", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SpendAndSaveResponse>> createSpendAndSave(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                    @Valid @RequestBody SpendAndSaveSetUpRequestJSON setUpRequestJSON) {
        SpendAndSaveResponse response = createSpendAndSaveUseCase.setUpSpendAndSave(authenticatedUser, setUpRequestJSON.toRequest());
        ApiResponseJSON<SpendAndSaveResponse> apiResponseJSON = new ApiResponseJSON<>("Your savings plan created successfully", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation("Fetches Spend and save details")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SpendAndSaveResponse>> getSpendAndSave(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        SpendAndSaveResponse response = getSpendAndSaveUseCase.getSpendAndSaveDashboard(authenticatedUser);
        if(authenticatedUser.getUserId().equalsIgnoreCase("700000000097")) {
            systemIssueLogService.logIssue("SPEND AND SAVE RESPONSE", "SPEND AND SAVE RESPONSE", gson.toJson(response));
        }
        ApiResponseJSON<SpendAndSaveResponse> apiResponseJSON = new ApiResponseJSON<>("Request processed successfully", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation("Updates status of the savings")
    @PutMapping(value = "/update-status", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SpendAndSaveResponse>> updateStatus(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                @RequestBody @Valid SpendAndSaveStatusUpdate statusUpdate) {

        SpendAndSaveResponse response = updateSpendAndSaveUseCase.updateSpendAndSaveStatus(authenticatedUser, statusUpdate.statusValue());
        ApiResponseJSON<SpendAndSaveResponse> apiResponseJSON = new ApiResponseJSON<>("Status updated successfully", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation("Edit spend and save percentage")
    @PostMapping(value = "/edit-savings", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<String>> editSpendAndSave(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                    @Valid @RequestBody EditSpendAndSaveRequestJSON requestJSON) {
        String response = updateSpendAndSaveUseCase.editSpendAndSaveSettings(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<String> apiResponseJSON = new ApiResponseJSON<>(response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation("Withdraw savings")
    @PostMapping(value = "/withdraw-funds", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<String>> withdrawSavings(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                   @Valid @RequestBody SpendAndSaveWithdrawalRequest withdrawalRequest) {

        String response = withdrawSpendAndSaveUseCase.withdrawSavings(authenticatedUser, withdrawalRequest);
        ApiResponseJSON<String> apiResponseJSON = new ApiResponseJSON<>(response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }




    @Data
    static class SpendAndSaveSetUpRequestJSON {
        @ApiModelProperty(notes = "Transaction percentage to be saved", required = true)
        private double transactionPercentage;

        @ApiModelProperty(notes = "This checks whether savings is locked", required = true)
        private boolean isSavingsLocked;

        @ApiModelProperty(notes = "The duration of the savings")
        private int duration;

        public SpendAndSaveSetUpRequest toRequest() {
            return SpendAndSaveSetUpRequest.builder()
                    .transactionPercentage(transactionPercentage)
                    .isSavingsLocked(isSavingsLocked)
                    .duration(duration)
                    .build();
        }
    }

    @Data
    static class SpendAndSaveStatusUpdate {
        @ApiModelProperty(notes = "Spend and Save Types: ACTIVE | INACTIVE", required = true)
        @NotBlank
        @NotNull
        @Pattern(regexp = "(ACTIVE|INACTIVE)", message = "Invalid status")
        String status;

        public boolean statusValue() {
            return status.equalsIgnoreCase("ACTIVE");
        }
    }
}
