package com.mintfintech.savingsms.infrastructure.web.controllers.backoffice;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.FundInvestmentByAdminJSON;
import com.mintfintech.savingsms.infrastructure.web.models.InvestmentCreationAdminRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.*;
import com.mintfintech.savingsms.usecase.features.investment.CreateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.FundInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.master_record.InvestmentPlanUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;

@Secured("ADMIN_PORTAL")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Api(tags = "Investment Admin Endpoints")
@RestController
@RequestMapping(value = "/api/v1/admin/investment", headers = {"x-request-client-key", "Authorization"})
@RequiredArgsConstructor
public class InvestmentAdminController {

    private final GetInvestmentUseCase getInvestmentUseCase;
    private final CreateInvestmentUseCase createInvestmentUseCase;
    private final InvestmentPlanUseCase investmentPlanUseCase;
    private final FundInvestmentUseCase fundInvestmentUseCase;


    @Secured("09") // Privilege: VIEW_DASHBOARD_STATISTICS
    @ApiOperation(value = "Returns investment maturity statistics information.")
    @GetMapping(value = "/maturity-statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<InvestmentMaturityStatSummary>> getSavingsMaturityStatistics(@ApiParam(value="Format: dd/MM/yyyy") @DateTimeFormat(pattern="dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                                       @ApiParam(value="Format: dd/MM/yyyy") @DateTimeFormat(pattern="dd/MM/yyyy")  @RequestParam(value = "toDate", required = false) LocalDate toDate) {
        if(fromDate == null || toDate == null) {
            fromDate = LocalDate.now();
            toDate = fromDate.plusWeeks(1); // default
        }
        InvestmentMaturityStatSummary response = getInvestmentUseCase.getMaturityStatistics(fromDate, toDate);
        ApiResponseJSON<InvestmentMaturityStatSummary> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured("19") // Privilege: CAN_VIEW_INVESTMENT
    @ApiOperation(value = "Returns paginated investment list.")
    @GetMapping(value = "/completed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<InvestmentModel>>> getCompletedInvestments(@ApiParam(value = "Investment Status: ALL, COMPLETED, LIQUIDATED") @Valid @Pattern(regexp = "(ALL|COMPLETED|LIQUIDATED)") @RequestParam(value = "investmentStatus", defaultValue = "ALL") String investmentStatus,
                                                                                                 @ApiParam(value = "Customer first or last name") @RequestParam(value = "customerName", required = false) String customerName,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startFromDate", required = false) LocalDate startFromDate,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startToDate", required = false) LocalDate startToDate,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureFromDate", required = false) LocalDate matureFromDate,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureToDate", required = false) LocalDate matureToDate,
                                                                                                 @ApiParam(value = "Duration of investment in months. Min:1") @Valid @Min(value = 0) @RequestParam(value = "duration", required = false, defaultValue = "0") int duration,
                                                                                                 @ApiParam(value = "No. of records per page. Min:1, Max:500") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
                                                                                                 @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {

        InvestmentSearchRequest searchRequest = InvestmentSearchRequest.builder()
                .investmentStatus(investmentStatus.equalsIgnoreCase("ALL")? "" : investmentStatus)
                .startToDate(startToDate)
                .customerName(StringUtils.isNotEmpty(customerName) ? customerName.trim() : null)
                .duration(duration)
                .matureFromDate(matureFromDate)
                .matureToDate(matureToDate)
                .startFromDate(startFromDate)
                .completedRecords(true)
                .build();

        InvestmentStatSummary response = getInvestmentUseCase.getPagedInvestments(searchRequest, page, size);

        ApiResponseJSON<PagedDataResponse<InvestmentModel>> apiResponseJSON = new ApiResponseJSON<>("Investment list returned successfully.", response.getInvestments());
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured("19") // Privilege: CAN_VIEW_INVESTMENT
    @ApiOperation(value = "Returns paginated investment list.")
    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<InvestmentModel>>> getAllInvestments(@ApiParam(value = "Customer first or last name") @RequestParam(value = "customerName", required = false) String customerName,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startFromDate", required = false) LocalDate startFromDate,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startToDate", required = false) LocalDate startToDate,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureFromDate", required = false) LocalDate matureFromDate,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureToDate", required = false) LocalDate matureToDate,
                                                                                                 @ApiParam(value = "Duration of investment in months. Min:1") @Valid @Min(value = 0) @RequestParam(value = "duration", required = false, defaultValue = "0") int duration,
                                                                                                 @ApiParam(value = "No. of records per page. Min:1, Max:500") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
                                                                                                 @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {


        InvestmentSearchRequest searchRequest = InvestmentSearchRequest.builder()
                .startToDate(startToDate)
                .customerName(StringUtils.isNotEmpty(customerName) ? customerName.trim() : null)
                .duration(duration)
                .matureFromDate(matureFromDate)
                .matureToDate(matureToDate)
                .startFromDate(startFromDate)
                .investmentStatus("ACTIVE")
                .completedRecords(false)
                .build();

        InvestmentStatSummary response = getInvestmentUseCase.getPagedInvestments(searchRequest, page, size);

        ApiResponseJSON<PagedDataResponse<InvestmentModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response.getInvestments());
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }


    @Secured("06") // Privilege: CAN_VIEW_CUSTOMER_INVESTMENT
    @ApiOperation(value = "Returns paginated investment list.")
    @GetMapping(value = "customer/{accountId}/completed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<InvestmentModel>>> getCustomerCompletedInvestments(@PathVariable(value = "accountId") String accountId,
                                                                                              @ApiParam(value = "Investment Status: ALL, COMPLETED, LIQUIDATED") @Valid
                                                                                              @Pattern(regexp = "(ALL|COMPLETED|LIQUIDATED)") @RequestParam(value = "investmentStatus", defaultValue = "ALL") String investmentStatus,
                                                                                              @ApiParam(value = "Customer first or last name") @RequestParam(value = "customerName", required = false) String customerName,
                                                                                              @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startFromDate", required = false) LocalDate startFromDate,
                                                                                              @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startToDate", required = false) LocalDate startToDate,
                                                                                              @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureFromDate", required = false) LocalDate matureFromDate,
                                                                                              @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureToDate", required = false) LocalDate matureToDate,
                                                                                              @ApiParam(value = "Duration of investment in months. Min:1") @Valid @Min(value = 0) @RequestParam(value = "duration", required = false, defaultValue = "0") int duration,
                                                                                              @ApiParam(value = "No. of records per page. Min:1, Max:500") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
                                                                                              @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {

        InvestmentSearchRequest searchRequest = InvestmentSearchRequest.builder()
                .accountId(accountId)
                .investmentStatus(investmentStatus.equalsIgnoreCase("ALL")? "" : investmentStatus)
                .startToDate(startToDate)
                .customerName(StringUtils.isNotEmpty(customerName) ? customerName.trim() : null)
                .duration(duration)
                .matureFromDate(matureFromDate)
                .matureToDate(matureToDate)
                .startFromDate(startFromDate)
                .completedRecords(true)
                .build();

        InvestmentStatSummary response = getInvestmentUseCase.getPagedInvestmentsByAdmin(searchRequest, page, size);

        ApiResponseJSON<PagedDataResponse<InvestmentModel>> apiResponseJSON = new ApiResponseJSON<>("Investment list returned successfully.", response.getInvestments());
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured("06") // Privilege: CAN_VIEW_CUSTOMER_INVESTMENT
    @ApiOperation(value = "Returns paginated investment list.")
    @GetMapping(value = "customer/{accountId}/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<InvestmentModel>>> getCustomerActiveInvestments(@PathVariable(value = "accountId") String accountId,
                                                                                              @ApiParam(value = "Customer first or last name") @RequestParam(value = "customerName", required = false) String customerName,
                                                                                              @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startFromDate", required = false) LocalDate startFromDate,
                                                                                              @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startToDate", required = false) LocalDate startToDate,
                                                                                              @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureFromDate", required = false) LocalDate matureFromDate,
                                                                                              @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureToDate", required = false) LocalDate matureToDate,
                                                                                              @ApiParam(value = "Duration of investment in months. Min:1") @Valid @Min(value = 0) @RequestParam(value = "duration", required = false, defaultValue = "0") int duration,
                                                                                              @ApiParam(value = "No. of records per page. Min:1, Max:500") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
                                                                                              @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {

        InvestmentSearchRequest searchRequest = InvestmentSearchRequest.builder()
                .accountId(accountId)
                .investmentStatus("ACTIVE")
                .startToDate(startToDate)
                .customerName(StringUtils.isNotEmpty(customerName) ? customerName.trim() : null)
                .duration(duration)
                .matureFromDate(matureFromDate)
                .matureToDate(matureToDate)
                .startFromDate(startFromDate)
                .completedRecords(false)
                .build();

        InvestmentStatSummary response = getInvestmentUseCase.getPagedInvestmentsByAdmin(searchRequest, page, size);

        ApiResponseJSON<PagedDataResponse<InvestmentModel>> apiResponseJSON = new ApiResponseJSON<>("Investment list returned successfully.", response.getInvestments());
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured("20") // Privilege: CAN_CREATE_INVESTMENT
    @ApiOperation(value = "Creates a new investment.")
    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<InvestmentCreationResponse>> createInvestment(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                        @RequestBody @Valid InvestmentCreationAdminRequestJSON requestJSON) {
        InvestmentCreationResponse response = createInvestmentUseCase.createInvestmentByAdmin(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<InvestmentCreationResponse> apiResponseJSON = new ApiResponseJSON<>("Investment created successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured("20")
    @ApiOperation(value = "Fund an investment.")
    @PostMapping(value = "/fund", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<InvestmentFundingResponse>> fundInvestment(@RequestBody @Valid FundInvestmentByAdminJSON requestJSON) {
        InvestmentFundingResponse response = fundInvestmentUseCase.fundInvestmentByAdmin(requestJSON.toRequest());
        ApiResponseJSON<InvestmentFundingResponse> apiResponseJSON = new ApiResponseJSON<>("Completed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

}
