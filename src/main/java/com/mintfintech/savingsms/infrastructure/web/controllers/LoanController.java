package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.LoanRepaymentUseCase;
import com.mintfintech.savingsms.usecase.LoanRequestUseCase;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.data.request.LoanSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

@FieldDefaults(makeFinal = true)
@Api(tags = "Loan Transaction Management Endpoints")
@RestController
@RequestMapping(value = "/api/v1/loan/", headers = {"x-request-client-key", "Authorization"})
@RequiredArgsConstructor
@Validated
public class LoanController {

    private final GetLoansUseCase getLoansUseCase;
    private final CustomerLoanProfileUseCase customerLoanProfileUseCase;
    private final LoanRequestUseCase loanRequestUseCase;
    private final LoanRepaymentUseCase loanRepaymentUseCase;

    @ApiOperation(value = "Returns customer loan profile.")
    @GetMapping(value = "customer-profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> getCustomerLoanProfile(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                            @Pattern(regexp = "(PAYDAY)") @NotEmpty @RequestParam("loanType") String loanType) {

        LoanCustomerProfileModel response = customerLoanProfileUseCase.getLoanCustomerProfile(authenticatedUser, loanType);
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns paginated list of loans of a user.")
    @GetMapping(value = "loan-history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<LoanModel>>> getLoans(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                  @Pattern(regexp = "(PAID|PARTIALLY_PAID|PENDING|FAILED)") @RequestParam("loanStatus") String loanStatus,
                                                                                  @Pattern(regexp = "(PAYDAY)") @RequestParam("loanType") String loanType,
                                                                                  @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                  @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                                  @RequestParam("size") int size,
                                                                                  @RequestParam("page") int page
    ) {

        LoanSearchRequest searchRequest = LoanSearchRequest.builder()
                .accountId(authenticatedUser.getAccountId())
                .loanStatus(loanStatus)
                .loanType(loanType)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        PagedDataResponse<LoanModel> response = getLoansUseCase.getPagedLoans(searchRequest, page, size);
        ApiResponseJSON<PagedDataResponse<LoanModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Add Employee Information to Customer Loan Profile And Request for Loan.")
    @PostMapping(value = "loan-request/payday", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanModel>> createEmployeeInformation(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                @NotNull @RequestParam("employmentLetter") MultipartFile employmentLetter,
                                                                                @NotEmpty @RequestParam("organizationName") String organizationName,
                                                                                @RequestParam(value = "monthlyIncome", defaultValue = "0.0") double monthlyIncome,
                                                                                @NotEmpty @RequestParam("organizationUrl") String organizationUrl,
                                                                                @NotEmpty @RequestParam("employerAddress") String employerAddress,
                                                                                @Email @NotEmpty @RequestParam("employerEmail") String employerEmail,
                                                                                @Pattern(regexp = "[0-9]{11}", message = "11 digits phone number is required.") @NotEmpty @RequestParam("employerPhoneNo") String employerPhoneNo,
                                                                                @Email @NotEmpty @RequestParam("workEmail") String workEmail,
                                                                                @Min(value = 1000, message = "Minimum of N1000") @NotNull @RequestParam("loanAmount") double loanAmount,
                                                                                @NotEmpty @RequestParam("creditAccountId") String creditAccountId) {

        EmploymentDetailCreationRequest request = EmploymentDetailCreationRequest.builder()
                .employmentLetter(employmentLetter)
                .employerAddress(employerAddress)
                .employerEmail(StringUtils.trim(employerEmail))
                .employerPhoneNo(StringUtils.trim(employerPhoneNo))
                .monthlyIncome(BigDecimal.valueOf(monthlyIncome))
                .organizationName(StringUtils.trim(organizationName))
                .organizationUrl(StringUtils.trim(organizationUrl))
                .workEmail(StringUtils.trim(workEmail))
                .loanAmount(loanAmount)
                .creditAccountId(creditAccountId)
                .build();

        LoanModel response = loanRequestUseCase.paydayLoanRequest(authenticatedUser, request);
        ApiResponseJSON<LoanModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Request for Loan.")
    @PostMapping(value = "loan-request", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanModel>> loanRequest(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                  @RequestBody @Valid LoanRequest request) {

        LoanModel response = loanRequestUseCase.loanRequest(authenticatedUser, request.getAmount(), request.getLoanType(), request.getCreditAccountId());
        ApiResponseJSON<LoanModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Pay back loan Loan.")
    @PostMapping(value = "repayment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanModel>> repayment(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                @RequestBody @Valid LoanPayBackRequest request) {

        LoanModel response = loanRepaymentUseCase.repayment(authenticatedUser, request.getAmount(), request.getLoanId());
        ApiResponseJSON<LoanModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns list of loan transactions.")
    @GetMapping(value = "{loanId}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanModel>> getLoanTransactions(@PathVariable("loanId") String loanId) {

        LoanModel response = getLoansUseCase.getLoanTransactions(loanId);
        ApiResponseJSON<LoanModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Data
    private static class LoanRequest {
        @NotNull
        private double amount;

        @Pattern(regexp = "(PAYDAY)")
        @NotEmpty
        private String loanType;

        @ApiModelProperty(notes = "The bank accountId to be credited", required = true)
        @NotEmpty
        private String creditAccountId;
    }

    @Data
    private static class LoanPayBackRequest {
        @NotNull
        private double amount;

        @NotEmpty
        private String loanId;
    }

}
