package com.mintfintech.savingsms.infrastructure.web.controllers.backoffice;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentStatSummary;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

@Secured("ADMIN_PORTAL")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Api(tags = "Investment Admin Endpoints")
@RestController
@RequestMapping(value = "/api/v1/admin/investment/", headers = {"x-request-client-key", "Authorization"})
@RequiredArgsConstructor
public class InvestmentAdminController {

    private final GetInvestmentUseCase getInvestmentUseCase;

    @ApiOperation(value = "Returns paginated investment list.")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<InvestmentModel>>> getAllInvestments(@ApiParam(value = "Investment Status: ALL, ACTIVE, COMPLETED, MATURED, LIQUIDATED") @Valid @Pattern(regexp = "(ALL|ACTIVE|COMPLETED|MATURED|LIQUIDATED)") @RequestParam(value = "investmentStatus", defaultValue = "ALL") String investmentStatus,
                                                                                                 @ApiParam(value = "Customer first or last name") @RequestParam(value = "customerName", required = false) String customerName,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startFromDate", required = false) LocalDate startFromDate,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startToDate", required = false) LocalDate startToDate,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureFromDate", required = false) LocalDate matureFromDate,
                                                                                                 @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureToDate", required = false) LocalDate matureToDate,
                                                                                                 @ApiParam(value = "Duration of investment in months. Min:1") @Valid @Min(value = 0) @RequestParam(value = "duration", required = false, defaultValue = "0") int duration,
                                                                                                 @ApiParam(value = "No. of records per page. Min:1, Max:20") @Valid @Min(value = 1) @Max(value = 20) @RequestParam("size") int size,
                                                                                                 @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {

        InvestmentSearchRequest searchRequest = InvestmentSearchRequest.builder()
                .investmentStatus(investmentStatus)
                .startToDate(startToDate)
                .customerName(customerName.trim())
                .duration(duration)
                .matureFromDate(matureFromDate)
                .matureToDate(matureToDate)
                .startFromDate(startFromDate)
                .build();

        InvestmentStatSummary response = getInvestmentUseCase.getPagedInvestments(searchRequest, page, size);

        ApiResponseJSON<PagedDataResponse<InvestmentModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response.getInvestments());
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
