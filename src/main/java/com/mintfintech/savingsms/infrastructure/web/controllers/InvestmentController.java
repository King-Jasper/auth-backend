package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.InvestmentCreationRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.features.investment.CreateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentCreationResponseModel;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

@FieldDefaults(makeFinal = true)
@Api(tags = "Investment Management Endpoints", description = "Handles investment transaction management.")
@RestController
@RequestMapping(value = "/api/v1/investment/", headers = {"x-request-client-key", "Authorization"})
@RequiredArgsConstructor
@Validated
public class InvestmentController {

    private final CreateInvestmentUseCase createInvestmentUseCase;
    private final GetInvestmentUseCase getInvestmentUseCase;

    @ApiOperation(value = "Creates a new investment.")
    @PostMapping(value = "create-investment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<InvestmentCreationResponseModel>> createInvestment(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                             @RequestBody @Valid InvestmentCreationRequestJSON requestJSON) {
        InvestmentCreationResponseModel response = createInvestmentUseCase.createInvestment(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<InvestmentCreationResponseModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns paginated list of investments of a user.")
    @GetMapping(value = "investment-history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<InvestmentModel>>> getLoans(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                        @ApiParam(value = "Investment Status: ALL, ACTIVE, COMPLETED") @Valid @Pattern(regexp = "(ALL|ACTIVE|COMPLETED)") @RequestParam(value = "investmentStatus", defaultValue = "ALL") String investmentStatus,
                                                                                        @ApiParam(value = "Investment Type: ALL, MUTUAL_INVESTMENT") @Valid @Pattern(regexp = "(ALL|MUTUAL_INVESTMENT)") @RequestParam(value = "investmentType", defaultValue = "ALL") String investmentType,
                                                                                        @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                        @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                                        @ApiParam(value = "No. of records per page. Min:1, Max:20") @Valid @Min(value = 1) @Max(value = 20) @RequestParam("size") int size,
                                                                                        @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {

        InvestmentSearchRequest searchRequest = InvestmentSearchRequest.builder()
                .accountId(authenticatedUser.getAccountId())
                .investmentStatus(investmentStatus)
                .investmentType(investmentType)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        PagedDataResponse<InvestmentModel> response = getInvestmentUseCase.getPagedInvestments(searchRequest, page, size);
        ApiResponseJSON<PagedDataResponse<InvestmentModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
