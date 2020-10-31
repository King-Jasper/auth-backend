package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.RoundUpSavingSetUpRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.RoundUpSavingResponse;
import com.mintfintech.savingsms.usecase.features.roundup_savings.CreateRoundUpSavingsUseCase;
import com.mintfintech.savingsms.usecase.features.roundup_savings.GetRoundUpSavingsUseCase;
import com.mintfintech.savingsms.usecase.features.roundup_savings.UpdateRoundUpSavingsUseCase;
import com.mintfintech.savingsms.usecase.models.RoundUpSavingsTransactionModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.experimental.FieldDefaults;
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
import javax.validation.constraints.Pattern;
/**
 * Created by jnwanya on
 * Sat, 31 Oct, 2020
 */
@FieldDefaults(makeFinal = true)
@Api(tags = "RoundUp Savings Management Endpoints",  description = "Handles roundup savings goal management.")
@RestController
@RequestMapping(headers = {"x-request-client-key", "Authorization"})
public class RoundUpSavingsController {

    private final String v2BaseUrl = "/api/v2/savings-goals";

    private final CreateRoundUpSavingsUseCase createRoundUpSavingsUseCase;
    private final UpdateRoundUpSavingsUseCase  updateRoundUpSavingsUseCase;
    private final GetRoundUpSavingsUseCase getRoundUpSavingsUseCase;
    public RoundUpSavingsController(CreateRoundUpSavingsUseCase createRoundUpSavingsUseCase, UpdateRoundUpSavingsUseCase updateRoundUpSavingsUseCase, GetRoundUpSavingsUseCase getRoundUpSavingsUseCase) {
        this.createRoundUpSavingsUseCase = createRoundUpSavingsUseCase;
        this.updateRoundUpSavingsUseCase = updateRoundUpSavingsUseCase;
        this.getRoundUpSavingsUseCase = getRoundUpSavingsUseCase;
    }


    @ApiOperation(value = "Get account roundup savings.")
    @GetMapping(value = v2BaseUrl+ "/roundup-savings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<RoundUpSavingResponse>> getRoundUpSavings(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        RoundUpSavingResponse response = getRoundUpSavingsUseCase.getAccountRoundUpSavings(authenticatedUser);
        ApiResponseJSON<RoundUpSavingResponse> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Setup roundup savings.")
    @PostMapping(value = v2BaseUrl+ "/roundup-savings", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<RoundUpSavingResponse>> setupRoundUpSavings(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                      @RequestBody @Valid RoundUpSavingsSetup roundUpSavingsSetup) {
        RoundUpSavingResponse response = createRoundUpSavingsUseCase.setupRoundUpSavings(authenticatedUser, roundUpSavingsSetup.toRequest());
        ApiResponseJSON<RoundUpSavingResponse> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Update RoundUp Type of roundup savings.")
    @PutMapping(value = v2BaseUrl+ "/roundup-savings/{id}/roundup-type", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<RoundUpSavingResponse>> updateRoundUpType(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                      @PathVariable Long id,
                                                                                      @RequestBody @Valid RoundUpTypeSetup roundUpTypeSetup) {
        RoundUpSavingResponse response = updateRoundUpSavingsUseCase.updateRoundUpType(authenticatedUser, id , roundUpTypeSetup.roundUpType);
        ApiResponseJSON<RoundUpSavingResponse> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Update Status of roundup savings.")
    @PutMapping(value = v2BaseUrl+ "/roundup-savings/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<RoundUpSavingResponse>> updateRoundUpSavingsStatus(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                    @PathVariable Long id,
                                                                                    @RequestBody @Valid RoundUpStatusUpdate statusUpdate) {
        RoundUpSavingResponse response = updateRoundUpSavingsUseCase.updateRoundUpSavingsStatus(authenticatedUser, id , statusUpdate.statusValue());
        ApiResponseJSON<RoundUpSavingResponse> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }


    @ApiOperation(value = "Get roundup savings transaction.")
    @GetMapping(value = v2BaseUrl+ "/roundup-savings/{id}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<RoundUpSavingsTransactionModel>>> getRoundUpSavingsTransaction(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                                           @PathVariable Long id, @RequestParam("size") int size, @RequestParam("page") int page) {
        PagedDataResponse<RoundUpSavingsTransactionModel> response = getRoundUpSavingsUseCase.getRoundUpSavingsTransaction(authenticatedUser, id, size, page);
        ApiResponseJSON<PagedDataResponse<RoundUpSavingsTransactionModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }


    @Data
    static class RoundUpSavingsSetup {
        @ApiModelProperty(notes = "RoundUp Types: NEAREST_HUNDRED | NEAREST_THOUSAND", required = true)
        @NotEmpty
        @NotNull
        @Pattern(regexp = "(NEAREST_HUNDRED|NEAREST_THOUSAND)", message = "Invalid round-up type")
        String roundUpType;

        @Min(value = 90, message = "Duration must be minimum of 90 days")
        int duration;

        public RoundUpSavingSetUpRequest toRequest() {
            return RoundUpSavingSetUpRequest.builder()
                    .roundUpType(roundUpType)
                    .duration(duration)
                    .build();
        }
    }

    @Data
    static class RoundUpTypeSetup {
        @ApiModelProperty(notes = "RoundUp Types: NEAREST_HUNDRED | NEAREST_THOUSAND", required = true)
        @NotEmpty
        @NotNull
        @Pattern(regexp = "(NEAREST_HUNDRED|NEAREST_THOUSAND)", message = "Invalid round-up type")
        String roundUpType;
    }

    @Data
    static class RoundUpStatusUpdate {
        @ApiModelProperty(notes = "RoundUp Types: ACTIVE | INACTIVE", required = true)
        @NotEmpty
        @NotNull
        @Pattern(regexp = "(ACTIVE|INACTIVE)", message = "Invalid status")
        String status;

        public boolean statusValue() {
            return status.equalsIgnoreCase("ACTIVE");
        }
    }
}
