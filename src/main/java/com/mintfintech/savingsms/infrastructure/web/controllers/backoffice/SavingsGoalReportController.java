package com.mintfintech.savingsms.infrastructure.web.controllers.backoffice;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.backoffice.GetSavingsTransactionUseCase;
import com.mintfintech.savingsms.usecase.data.request.SavingsSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.PortalSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.data.response.SavingsMaturityStatSummary;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Sat, 06 Jun, 2020
 */
@Secured("ADMIN_PORTAL")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Api(tags = "Savings Goal Report Endpoints (BO)",  description = "Back-office: Handles savings goal report management.")
@RestController
@RequestMapping(value = "/api/v1/admin/", headers = {"x-request-client-key", "Authorization"})
@AllArgsConstructor
public class SavingsGoalReportController {

    GetSavingsGoalUseCase getSavingsGoalUseCase;
    GetSavingsTransactionUseCase getSavingsTransactionUseCase;

    @Secured("08") // Privilege: VIEW_TRANSACTION_REPORTS
    @ApiOperation(value = "Returns paginated list of savings goal.")
    @GetMapping(value = "savings-goals", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<PortalSavingsGoalResponse>>> getSavingsGoal(@RequestParam(value = "accountId", required = false) String accountId, @RequestParam(value = "goalId", required = false) String goalId,
                                                                                             @NotBlank @Pattern(regexp = "(ACTIVE|MATURED|COMPLETED)") @RequestParam(value = "goalStatus", defaultValue = "ACTIVE") String goalStatus,
                                                                                             @NotBlank @Pattern(regexp = "(ALL|ENABLED|DISABLED)") @RequestParam(value = "autoSaveStatus", defaultValue = "ALL") String autoSaveStatus,
                                                                                             @Pattern(regexp = "(ALL|ROUND_UP_SAVINGS|CUSTOMER_SAVINGS|EMERGENCY_SAVINGS|MINT_REFERRAL_EARNINGS)") @RequestParam(value = "savingsType", required = false, defaultValue = "ALL") String savingsType,
                                                                                             @ApiParam(value="Format: dd/MM/yyyy")  @DateTimeFormat(pattern="dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                             @ApiParam(value="Format: dd/MM/yyyy")  @DateTimeFormat(pattern="dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                                             @RequestParam("size") int size, @RequestParam("page") int page) {
        if(size > 20) {
            size = 20;
        }
        SavingsSearchRequest searchRequest = SavingsSearchRequest.builder()
                .goalId(goalId).savingsStatus(goalStatus)
                .accountId(accountId)
                .savingsType(savingsType)
                .fromDate(fromDate).toDate(toDate)
                .autoSavedStatus(autoSaveStatus)
                .build();
        PagedDataResponse<PortalSavingsGoalResponse> response = getSavingsGoalUseCase.getPagedSavingsGoals(searchRequest, page, size);
        ApiResponseJSON<PagedDataResponse<PortalSavingsGoalResponse>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
    @Secured("06") // Privilege: CAN_VIEW_CUSTOMER_INFORMATION
    @ApiOperation(value = "Returns paginated list of customer savings goal using accountId.")
    @GetMapping(value = "customer/{accountId}/savings-goal", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<PortalSavingsGoalResponse>>> getCustomerSavingsGoalByAccountId(@PathVariable(value = "accountId") String accountId,
                                                                                                                           @NotBlank @Pattern(regexp = "(ACTIVE|MATURED|COMPLETED)") @RequestParam(value = "goalStatus", defaultValue = "ACTIVE") String goalStatus,
                                                                                                                           @NotBlank @Pattern(regexp = "(ALL|ENABLED|DISABLED)") @RequestParam(value = "autoSaveStatus", defaultValue = "ALL") String autoSaveStatus,
                                                                                                                           @ApiParam(value="Format: dd/MM/yyyy")  @DateTimeFormat(pattern="dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                                                           @ApiParam(value="Format: dd/MM/yyyy")  @DateTimeFormat(pattern="dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                                                                           @Valid @Min(value = 1) @Max(value = 20) @RequestParam("size") int size,
                                                                                                                           @Valid @Min(value = 0) @RequestParam("page") int page) {

        SavingsSearchRequest searchRequest = SavingsSearchRequest.builder()
                .savingsStatus(goalStatus)
                .accountId(accountId)
                .fromDate(fromDate)
                .toDate(toDate)
                .autoSavedStatus(autoSaveStatus)
                .build();
        PagedDataResponse<PortalSavingsGoalResponse> response = getSavingsGoalUseCase.getPagedSavingsGoals(searchRequest, page, size);
        ApiResponseJSON<PagedDataResponse<PortalSavingsGoalResponse>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }


    @Secured("08") // Privilege: VIEW_TRANSACTION_REPORTS
    @ApiOperation(value = "Returns savings goal details by goal id.")
    @GetMapping(value = "savings-goals/{goalId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PortalSavingsGoalResponse>> getSavingsGoal(@PathVariable("goalId") String goalId) {

        PortalSavingsGoalResponse response = getSavingsGoalUseCase.getPortalSavingsGoalResponseByGoalId(goalId);
        ApiResponseJSON<PortalSavingsGoalResponse> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured("09") // Privilege: VIEW_DASHBOARD_STATISTICS
    @ApiOperation(value = "Returns savings maturity statistics information.")
    @GetMapping(value = "savings-goals/maturity-statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<SavingsMaturityStatSummary>> getSavingsMaturityStatistics(@ApiParam(value="Format: dd/MM/yyyy") @DateTimeFormat(pattern="dd/MM/yyyy") @RequestParam(value = "fromDate") LocalDate fromDate,
                                                                                                            @ApiParam(value="Format: dd/MM/yyyy") @DateTimeFormat(pattern="dd/MM/yyyy")  @RequestParam(value = "toDate") LocalDate toDate) {
        SavingsMaturityStatSummary response = getSavingsTransactionUseCase.getSavingsMaturityStatistics(fromDate, toDate);
        ApiResponseJSON<SavingsMaturityStatSummary> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

}
