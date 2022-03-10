package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.BusinessLoanResponse;
import com.mintfintech.savingsms.usecase.data.response.LoanDashboardResponse;
import com.mintfintech.savingsms.usecase.data.response.LoanRequestScheduleResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.features.loan.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.features.loan.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.features.loan.LoanRepaymentUseCase;
import com.mintfintech.savingsms.usecase.features.loan.LoanRequestUseCase;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.data.request.LoanSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.features.loan.business_loan.CreateBusinessLoanUseCase;
import com.mintfintech.savingsms.usecase.features.loan.business_loan.GetBusinessLoanUseCase;
import com.mintfintech.savingsms.usecase.models.*;
import com.mintfintech.savingsms.utils.EmailUtils;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    private final CreateBusinessLoanUseCase createBusinessLoanUseCase;
    private final GetBusinessLoanUseCase getBusinessLoanUseCase;

    @ApiOperation(value = "Returns customer loan profile.")
    @GetMapping(value = "customer-profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<CustomerLoanProfileDashboard>> getCustomerLoanProfile(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                @ApiParam(value = "Loan Type: PAYDAY") @Pattern(regexp = "(PAYDAY)") @NotEmpty @RequestParam("loanType") String loanType) {

        CustomerLoanProfileDashboard response = customerLoanProfileUseCase.getLoanCustomerProfileDashboard(authenticatedUser, loanType);
        ApiResponseJSON<CustomerLoanProfileDashboard> apiResponseJSON = new ApiResponseJSON<>("Customer profile returned successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

//    @ApiOperation(value = "Returns customer employment information.")
//    @GetMapping(value = "customer-profile/employment-info", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<ApiResponseJSON<EmploymentInformationModel>> getEmploymentInfo(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser){
//
//        EmploymentInformationModel response = customerLoanProfileUseCase.getEmploymentInfo(authenticatedUser);
//        ApiResponseJSON<EmploymentInformationModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
//        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
//    }

    @ApiOperation(value = "Returns paginated list of loans of a user.")
    @GetMapping(value = "loan-history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<LoanModel>>> getLoans(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                  @ApiParam(value = "Repayment Status: ALL, PAID, PARTIALLY_PAID, PENDING, FAILED, CANCELLED") @Valid @Pattern(regexp = "(ALL|PAID|PARTIALLY_PAID|PENDING|FAILED|CANCELLED)") @RequestParam(value = "repaymentStatus", defaultValue = "ALL") String repaymentStatus,
                                                                                  @ApiParam(value = "Approval Status: ALL, APPROVED, REJECTED, PENDING, CANCELLED, DISBURSED") @Valid @Pattern(regexp = "(ALL|APPROVED|REJECTED|PENDING|CANCELLED|DISBURSED)") @RequestParam(value = "approvalStatus", defaultValue = "ALL") String approvalStatus,
                                                                                  @ApiParam(value = "Loan Type: PAYDAY") @Pattern(regexp = "(PAYDAY)") @NotEmpty @Pattern(regexp = "(PAYDAY)") @RequestParam("loanType") String loanType,
                                                                                  @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                  @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                                  @ApiParam(value = "No. of records per page. Min:1, Max:20") @Valid @Min(value = 1) @Max(value = 20) @RequestParam("size") int size,
                                                                                  @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {

        LoanSearchRequest searchRequest = LoanSearchRequest.builder()
                .accountId(authenticatedUser.getAccountId())
                .repaymentStatus(repaymentStatus)
                .approvalStatus(approvalStatus)
                .loanType(loanType)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        PagedDataResponse<LoanModel> response = getLoansUseCase.getPagedLoans(searchRequest, page, size);
        ApiResponseJSON<PagedDataResponse<LoanModel>> apiResponseJSON = new ApiResponseJSON<>("Loan history returned successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Create Customer Loan Profile for Pay Day Loan.")
    @PostMapping(value = "customer-profile/payday", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> createEmploymentInformation(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                @ApiParam(value = "Upload Employment Letter", required = true) @NotNull @RequestParam("employmentLetter") MultipartFile employmentLetter,
                                                                                @ApiParam(value = "Organization Name", required = true) @NotEmpty @RequestParam("organizationName") String organizationName,
                                                                                @ApiParam(value = "Monthly net income. Min:1000", required = true) @RequestParam(value = "monthlyIncome", defaultValue = "0.0") double monthlyIncome,
                                                                                @ApiParam(value = "Organization website", required = true) @NotEmpty @RequestParam("organizationWebsite") String organizationUrl,
                                                                                @ApiParam(value = "Employer Address", required = true) @NotEmpty @RequestParam("employerAddress") String employerAddress,
                                                                                @ApiParam(value = "Employer Email", required = true)  @NotEmpty @RequestParam("employerEmail") String employerEmail,
                                                                                @ApiParam(value = "Employer Phone Number", required = true) @NotEmpty @RequestParam("employerPhoneNo") String employerPhoneNo,
                                                                                @ApiParam(value = "Customer Work Email", required = true)  @NotEmpty @RequestParam("workEmail") String workEmail) {

        if(StringUtils.isNotEmpty(workEmail) && !EmailUtils.isValid(workEmail)) {
            throw new BadRequestException("Invalid email address provided.");
        }
        if(StringUtils.isNotEmpty(employerEmail) && !EmailUtils.isValid(employerEmail)) {
            throw new BadRequestException("Invalid email address provided.");
        }
        EmploymentDetailCreationRequest request = EmploymentDetailCreationRequest.builder()
                .employmentLetter(employmentLetter)
                .employerAddress(employerAddress)
                .employerEmail(StringUtils.trim(employerEmail))
                .employerPhoneNo(StringUtils.trim(employerPhoneNo))
                .monthlyIncome(BigDecimal.valueOf(monthlyIncome))
                .organizationName(StringUtils.trim(organizationName))
                .organizationUrl(StringUtils.trim(organizationUrl))
                .workEmail(StringUtils.trim(workEmail))
                .build();

        LoanCustomerProfileModel response = customerLoanProfileUseCase.createPaydayCustomerLoanProfile(authenticatedUser, request);
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Customer loan profile created successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Update Customer Employment Information.")
    @PutMapping(value = "customer-profile/employment-info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> updateEmploymentInformation(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                @ApiParam(value = "Upload Employment Letter") @RequestParam(value = "employmentLetter", required = false) MultipartFile employmentLetter,
                                                                                @ApiParam(value = "Organization Name", required = true) @NotEmpty @RequestParam("organizationName") String organizationName,
                                                                                @ApiParam(value = "Monthly net income. Min:1000", required = true) @RequestParam(value = "monthlyIncome", defaultValue = "0.0") double monthlyIncome,
                                                                                @ApiParam(value = "Organization website", required = true) @NotEmpty @RequestParam("organizationWebsite") String organizationUrl,
                                                                                @ApiParam(value = "Employer Address", required = true) @NotEmpty @RequestParam("employerAddress") String employerAddress,
                                                                                @ApiParam(value = "Employer Email", required = true)  @NotEmpty @RequestParam("employerEmail") String employerEmail,
                                                                                @ApiParam(value = "Employer Phone Number", required = true) @NotEmpty @RequestParam("employerPhoneNo") String employerPhoneNo,
                                                                                @ApiParam(value = "Customer Work Email", required = true)  @NotEmpty @RequestParam("workEmail") String workEmail) {

        if(StringUtils.isNotEmpty(workEmail) && !EmailUtils.isValid(workEmail)) {
            throw new BadRequestException("Invalid email address provided.");
        }
        if(StringUtils.isNotEmpty(employerEmail) && !EmailUtils.isValid(employerEmail)) {
            throw new BadRequestException("Invalid email address provided.");
        }
        EmploymentDetailCreationRequest request = EmploymentDetailCreationRequest.builder()
                .employmentLetter(employmentLetter)
                .employerAddress(employerAddress)
                .employerEmail(StringUtils.trim(employerEmail))
                .employerPhoneNo(StringUtils.trim(employerPhoneNo))
                .monthlyIncome(BigDecimal.valueOf(monthlyIncome))
                .organizationName(StringUtils.trim(organizationName))
                .organizationUrl(StringUtils.trim(organizationUrl))
                .workEmail(StringUtils.trim(workEmail))
                .build();

        LoanCustomerProfileModel response = customerLoanProfileUseCase.updateCustomerEmploymentInformation(authenticatedUser, request);
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Customer Employment Information updated successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }


    @ApiOperation(value = "Request for Loan.")
    @PostMapping(value = "loan-request", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanModel>> loanRequest(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                  @RequestBody @Valid LoanRequest request) {
      //
         LoanModel response = loanRequestUseCase.loanRequest(authenticatedUser, request.getAmount(), request.getLoanType(), request.getCreditAccountId());
        ApiResponseJSON<LoanModel> apiResponseJSON = new ApiResponseJSON<>("Loan request processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns list of loan transactions.")
    @GetMapping(value = "dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanDashboardResponse>> getLoanDashboard(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        LoanDashboardResponse response = customerLoanProfileUseCase.getLoanDashboardInformation(authenticatedUser);
        ApiResponseJSON<LoanDashboardResponse> apiResponseJSON = new ApiResponseJSON<>("Loan dashboard retrieved.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Loan schedule.")
    @GetMapping(value = "business/loan-request-schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanRequestScheduleResponse>> businessLoanRequestSchedule(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                    @RequestParam("amount") double amount,@RequestParam("duration") int duration) {

        LoanRequestScheduleResponse response = getBusinessLoanUseCase.getRepaymentSchedule(authenticatedUser, BigDecimal.valueOf(amount), duration);
        ApiResponseJSON<LoanRequestScheduleResponse> apiResponseJSON = new ApiResponseJSON<>("Retrieved successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
    @ApiOperation(value = "Create a business loan.")
    @PostMapping(value = "business/loan-request", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<BusinessLoanResponse>> businessLoanRequest(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                     @RequestBody @Valid BusinessLoanRequest request) {

        BusinessLoanResponse response = createBusinessLoanUseCase.createRequest(authenticatedUser, request.amount, request.durationInMonths, request.creditAccountId);
        String message = "Dear customer, your loan application is received and will be reviewed shortly. This will take between 2 - 5 days.";
        ApiResponseJSON<BusinessLoanResponse> apiResponseJSON = new ApiResponseJSON<>(message, response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
    @ApiOperation(value = "Returns list of business loan request.")
    @GetMapping(value = "business/loan-request", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<BusinessLoanResponse>>> getBusinessRequest(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                       @ApiParam(value = "No. of records per page. Min:1, Max:20") @Valid @Min(value = 1) @Max(value = 20) @RequestParam("size") int size,
                                                                                                       @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page) {
        PagedDataResponse<BusinessLoanResponse> response = getBusinessLoanUseCase.getBusinessLoanDetails(authenticatedUser, page, size);
        ApiResponseJSON<PagedDataResponse<BusinessLoanResponse>> apiResponseJSON = new ApiResponseJSON<>("Loan request retrieved successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Pay back loan.")
    @PostMapping(value = "repayment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanModel>> repayment(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                @RequestBody @Valid LoanPayBackRequest request) {
        LoanModel response = loanRepaymentUseCase.repayment(authenticatedUser, request.getAmount(), request.getLoanId());
        ApiResponseJSON<LoanModel> apiResponseJSON = new ApiResponseJSON<>("Loan repayment processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns list of loan transactions.")
    @GetMapping(value = "{loanId}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<LoanTransactionModel>>> getLoanTransactions(@PathVariable(value = "loanId") String loanId) {

        List<LoanTransactionModel> response = getLoansUseCase.getLoanTransactions(loanId);
        ApiResponseJSON<List<LoanTransactionModel>> apiResponseJSON = new ApiResponseJSON<>("Loan transactions processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Data
    private static class LoanRequest {
        @ApiModelProperty(notes = "The loan amount to request. N10000 minimum", required = true)
        @Min(value = 10000, message = "Minimum amount for loan is N10000")
        @NotNull
        private double amount;

        @ApiModelProperty(notes = " PAYDAY | BUSINESS", required = true)
        @Pattern(regexp = "(PAYDAY|BUSINESS)")
        @NotEmpty
        private String loanType;

        @ApiModelProperty(notes = "The bank accountId to be credited", required = true)
        @NotEmpty
        private String creditAccountId;
    }

    @Data
    private static class BusinessLoanRequest {
        @ApiModelProperty(notes = "The loan amount to request. N10000 minimum", required = true)
        @Min(value = 10000, message = "Minimum amount for loan is N10000")
        @NotNull
        private BigDecimal amount;

        private int durationInMonths;

        @ApiModelProperty(notes = "The bank accountId to be credited", required = true)
        @NotEmpty
        private String creditAccountId;
    }

    @Data
    private static class LoanPayBackRequest {
        @ApiModelProperty(notes = "The amount to pay back. N1000 minimum", required = true)
        @Min(value = 1000, message = "Minimum amount is N1000")
        @NotNull
        private double amount;

        @ApiModelProperty(notes = "The loan Id.", required = true)
        @NotEmpty
        private String loanId;
    }

}
