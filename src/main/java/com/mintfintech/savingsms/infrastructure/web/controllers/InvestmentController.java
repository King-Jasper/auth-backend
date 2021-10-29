package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.InvestmentCreationRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.InvestmentFundingRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentWithdrawalRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentCreationResponse;
import com.mintfintech.savingsms.usecase.data.response.InvestmentFundingResponse;
import com.mintfintech.savingsms.usecase.data.response.InvestmentStatSummary;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.features.corporate.ManageTransactionRequestUseCase;
import com.mintfintech.savingsms.usecase.features.investment.CreateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.FundInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.WithdrawalInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import com.mintfintech.savingsms.usecase.models.InvestmentTransactionModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
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
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@FieldDefaults(makeFinal = true)
@Api(tags = "Investment Management Endpoints", description = "Handles investment transaction management.")
@RestController
@RequestMapping(value = "/api/v1/investments", headers = {"x-request-client-key", "Authorization"})
@RequiredArgsConstructor
@Validated
public class InvestmentController {

    private final CreateInvestmentUseCase createInvestmentUseCase;
    private final FundInvestmentUseCase fundInvestmentUseCase;
    private final GetInvestmentUseCase getInvestmentUseCase;
    private final WithdrawalInvestmentUseCase withdrawalInvestmentUseCase;
    private final ManageTransactionRequestUseCase corporateInvestmentUseCase;

    @ApiOperation(value = "Creates a new investment.")
    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<InvestmentCreationResponse>> createInvestment(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                        @RequestBody @Valid InvestmentCreationRequestJSON requestJSON) {
        InvestmentCreationResponse response = createInvestmentUseCase.createInvestment(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<InvestmentCreationResponse> apiResponseJSON = new ApiResponseJSON<>("Investment created successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns paginated list of investments of a user.")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<InvestmentStatSummary>> getInvestments(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                           @ApiParam(value = "Investment Status: ALL, ACTIVE, COMPLETED") @Valid @Pattern(regexp = "(ALL|ACTIVE|COMPLETED)") @RequestParam(value = "investmentStatus", defaultValue = "ALL") String investmentStatus,
                                                                           @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                           @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                           @ApiParam(value = "No. of records per page. Min:1, Max:500") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
                                                                           @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {

        InvestmentSearchRequest searchRequest = InvestmentSearchRequest.builder()
                .accountId(authenticatedUser.getAccountId())
                .investmentStatus(investmentStatus)
                .startFromDate(fromDate)
                .startToDate(toDate)
                .build();
        //System.out.println(searchRequest.toString());

        InvestmentStatSummary response = getInvestmentUseCase.getPagedInvestments(searchRequest, page, size);
        ApiResponseJSON<InvestmentStatSummary> apiResponseJSON = new ApiResponseJSON<>("Customer investment returned successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns list of investment transaction history.")
    @GetMapping(value = "{investmentId}/transaction-history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<InvestmentTransactionModel>>> getInvestmentTransactions(@PathVariable(value = "investmentId") String investmentId) {

        List<InvestmentTransactionModel> response = getInvestmentUseCase.getInvestmentTransactions(investmentId);
        ApiResponseJSON<List<InvestmentTransactionModel>> apiResponseJSON = new ApiResponseJSON<>("Investment history returned successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
    
    @ApiOperation(value = "Fund an investment.")
    @PostMapping(value = "/fund", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<InvestmentFundingResponse>> fundInvestment(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                        @RequestBody @Valid FundInvestmentJSON requestJSON) {
        InvestmentFundingResponse response = fundInvestmentUseCase.fundInvestment(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<InvestmentFundingResponse> apiResponseJSON = new ApiResponseJSON<>("Investment funded successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Liquidate an investment.")
    @PostMapping(value = "/liquidate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<InvestmentModel>> liquidateInvestment(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                @RequestBody @Valid LiquidateInvestmentJSON requestJSON) {
        InvestmentModel response = withdrawalInvestmentUseCase.liquidateInvestment(authenticatedUser, requestJSON.toRequest());
        ApiResponseJSON<InvestmentModel> apiResponseJSON = new ApiResponseJSON<>("Investment liquidated successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Data
    static class FundInvestmentJSON {
        @NotEmpty(message = "Investment code is required.")
        private String investmentCode;
        @NotEmpty(message = "Debit Account is required.")
        private String debitAccountId;
        @NotNull(message = "Amount is required.")
        private BigDecimal amount;
        @ApiModelProperty(value = "Transaction pin")
        private String transactionPin;

        public InvestmentFundingRequest toRequest() {
            return InvestmentFundingRequest.builder()
                    .investmentCode(investmentCode)
                    .debitAccountId(debitAccountId)
                    .amount(amount)
                    .transactionPin(transactionPin)
                    .build();
        }
    }

    @Data
    static class LiquidateInvestmentJSON {
        @NotEmpty(message = "Investment code is required.")
        private String investmentCode;

        @NotEmpty(message = "Debit Account is required.")
        private String creditAccountId;

        private boolean fullLiquidation;

        @ApiModelProperty(notes = "The amount to be liquidated. Required if fullLiquidation is false")
        private BigDecimal liquidationAmount;

        public InvestmentWithdrawalRequest toRequest() {
            if(!fullLiquidation && (liquidationAmount == null || liquidationAmount.compareTo(BigDecimal.ZERO) == 0)) {
                throw new BadRequestException("Liquidation amount must be provided.");
            }
            return InvestmentWithdrawalRequest.builder()
                    .investmentCode(investmentCode)
                    .creditAccountId(creditAccountId)
                    .fullLiquidation(fullLiquidation)
                    .amount(liquidationAmount)
                    .build();
        }
    }
}
