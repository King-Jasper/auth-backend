package com.mintfintech.savingsms.infrastructure.web.controllers.backoffice;

import com.mintfintech.savingsms.domain.models.PagedResponse;
import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.HNICustomerCreationRequest;
import com.mintfintech.savingsms.usecase.data.request.HNICustomerSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.LoanRequestScheduleResponse;
import com.mintfintech.savingsms.usecase.data.response.RepaymentSchedule;
import com.mintfintech.savingsms.usecase.features.loan.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.features.loan.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.features.loan.HNILoanUseCases;
import com.mintfintech.savingsms.usecase.features.loan.LoanApprovalUseCase;
import com.mintfintech.savingsms.usecase.data.request.CustomerProfileSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.LoanSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.HNILoanCustomerModel;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import com.mintfintech.savingsms.usecase.models.LoanTransactionModel;
import com.mintfintech.savingsms.utils.PhoneNumberUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AccessLevel;
import lombok.Data;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

@Secured("ADMIN_PORTAL")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Api(tags = "Loan Admin Endpoints")
@RestController
@RequestMapping(value = "/api/v1/admin/loan/", headers = {"x-request-client-key", "Authorization"})
@RequiredArgsConstructor
public class LoanAdminController {

    private final CustomerLoanProfileUseCase customerLoanProfileUseCase;
    private final GetLoansUseCase getLoansUseCase;
    private final LoanApprovalUseCase loanApprovalUseCase;
    private final HNILoanUseCases hniLoanUseCases;


    @Secured({"29", "30"}) // LOAN_PRODUCT_OFFICER | LOAN_RISK_OFFICER
    @ApiOperation(value = "Verify Loan Customer Employment Information.")
    @PutMapping(value = "{customerLoanProfileId}/verify/employment-details", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> verifyEmploymentInformation(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                 @PathVariable(value = "customerLoanProfileId") String customerLoanProfileId,
                                                                                                 @RequestBody @Valid ProfileVerificationRequest request) {

        LoanCustomerProfileModel response = customerLoanProfileUseCase.verifyEmploymentInformation(authenticatedUser, Long.parseLong(customerLoanProfileId), Boolean.parseBoolean(request.getVerified()), request.getReason());
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Loan Customer Employment Information verified successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured({"29", "30"}) // LOAN_PRODUCT_OFFICER | LOAN_RISK_OFFICER
    @ApiOperation(value = "Blacklist a customer.")
    @PutMapping(value = "{customerLoanProfileId}/blacklist", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> blackListCustomer(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                       @PathVariable(value = "customerLoanProfileId") String customerLoanProfileId,
                                                                                       @RequestBody @Valid BlacklistRequest request) {

        LoanCustomerProfileModel response = customerLoanProfileUseCase.blackListCustomer(authenticatedUser, Long.parseLong(customerLoanProfileId), Boolean.parseBoolean(request.getBlacklist()), request.getReason());
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Customer blacklisted successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured({"31", "32"}) // LOAN_FINANCE_OFFICER | LOAN_BUSINESS_MANAGER
    @ApiOperation(value = "Approve/Reject Loan Request.")
    @PostMapping(value = "{loanId}/approve", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanModel>> approveLoan(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                  @PathVariable(value = "loanId") String loanId,
                                                                  @RequestBody @Valid LoanApprovalRequest request) {

        LoanModel response = loanApprovalUseCase.approveLoanRequest(authenticatedUser, loanId, request.getReason(), Boolean.parseBoolean(request.getApproved()));
        ApiResponseJSON<LoanModel> apiResponseJSON = new ApiResponseJSON<>("Request completed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
    @Secured("28") // CAN_VIEW_LOAN_RECORDS
    @ApiOperation(value = "Returns list of hni customers.")
    @GetMapping(value = "hni-customers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedResponse<HNILoanCustomerModel>>> getHNICustomers(@ApiParam(value = "Repayment Plan: ALL, PRORATED_PRINCIPAL_INTEREST, END_OF_TENURE, INTEREST_ONLY") @Valid @Pattern(regexp = "(ALL|PRORATED_PRINCIPAL_INTEREST|END_OF_TENURE|INTEREST_ONLY)")
                                                                                           @RequestParam(value = "repaymentPlanType", defaultValue = "ALL") String repaymentPlanType,
                                                                                                @RequestParam(value = "customerNameOrAccountNumber", defaultValue = "", required = false) String customerNameOrAccountNumber,
                                                                                                @ApiParam(value = "No. of records per page. Min:1, Max:20") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
                                                                                                @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page) {

        HNICustomerSearchRequest searchRequest = HNICustomerSearchRequest.builder()
                .customerNameOrAccountNumber(customerNameOrAccountNumber)
                .repaymentType(repaymentPlanType.equalsIgnoreCase("ALL") ? "" : repaymentPlanType)
                .build();
        PagedResponse<HNILoanCustomerModel> response = hniLoanUseCases.getHNICustomers(searchRequest, page, size);
        ApiResponseJSON<PagedResponse<HNILoanCustomerModel>> apiResponseJSON = new ApiResponseJSON<>("Retrieved successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured({"31", "32"}) // LOAN_FINANCE_OFFICER | LOAN_BUSINESS_MANAGER
    @ApiOperation(value = "Create HNI Loan Customer.")
    @PutMapping(value = "hni-customers", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<HNILoanCustomerModel>> createHNILoanCustomer(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                  @RequestBody @Valid HNICustomerCreationRequestJSON request) {

        HNILoanCustomerModel response = hniLoanUseCases.createHNICustomer(authenticatedUser, request.toRequest());
        ApiResponseJSON<HNILoanCustomerModel> apiResponseJSON = new ApiResponseJSON<>("Request completed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured("28") // CAN_VIEW_LOAN_RECORDS
    @ApiOperation(value = "Returns list of loan transactions.")
    @GetMapping(value = "{loanId}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<LoanTransactionModel>>> getLoanTransactions(@PathVariable(value = "loanId") String loanId) {

        List<LoanTransactionModel> response = getLoansUseCase.getLoanTransactions(loanId);
        ApiResponseJSON<List<LoanTransactionModel>> apiResponseJSON = new ApiResponseJSON<>("Loan transactions processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
    @Secured("28") // CAN_VIEW_LOAN_RECORDS
    @ApiOperation(value = "Returns list of loan repayment schedule.")
    @GetMapping(value = "{loanId}/repayment-schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<RepaymentSchedule>>> getLoanRepaymentSchedules(@PathVariable(value = "loanId") String loanId) {
        List<RepaymentSchedule> response = getLoansUseCase.getLoanRepaymentSchedules(loanId);
        ApiResponseJSON<List<RepaymentSchedule>> apiResponseJSON = new ApiResponseJSON<>("Loan transactions processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured("28") // CAN_VIEW_LOAN_RECORDS
    @ApiOperation(value = "Returns paginated loan list.")
    @GetMapping(value = "loans", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<LoanModel>>> getAllLoans(@ApiParam(value = "Repayment Status: ALL, PAID, PARTIALLY_PAID, PENDING, FAILED, CANCELLED") @Valid @Pattern(regexp = "(ALL|PAID|PARTIALLY_PAID|PENDING|FAILED|CANCELLED)") @RequestParam(value = "repaymentStatus", defaultValue = "ALL") String repaymentStatus,
                                                                                     @ApiParam(value = "Approval Status: ALL, APPROVED, DECLINED, PENDING, CANCELLED, DISBURSED") @Valid @Pattern(regexp = "(ALL|APPROVED|DECLINED|PENDING|CANCELLED|DISBURSED)") @RequestParam(value = "approvalStatus", defaultValue = "ALL") String approvalStatus,
                                                                                     @ApiParam(value = "Review Stage: ALL, FIRST_REVIEW, SECOND_REVIEW") @Valid @Pattern(regexp = "(ALL|FIRST_REVIEW|SECOND_REVIEW|THIRD_REVIEW)") @RequestParam(value = "reviewStage", defaultValue = "FIRST_REVIEW") String reviewStage,
                                                                                     @RequestParam(value = "customerName", defaultValue = "", required = false) String customerName,
                                                                                     @RequestParam(value = "customerPhone", defaultValue = "", required = false) String customerPhone,
                                                                                     @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                     @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                                     @ApiParam(value = "No. of records per page. Min:1, Max:20") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
                                                                                     @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {

        if(StringUtils.isNotEmpty(customerPhone)){
            customerPhone = PhoneNumberUtils.toInternationalFormat(customerPhone);
        }
        LoanSearchRequest searchRequest = LoanSearchRequest.builder()
                .repaymentStatus(repaymentStatus)
                .fromDate(fromDate)
                .toDate(toDate)
                .approvalStatus(approvalStatus)
                .reviewStage(reviewStage.equalsIgnoreCase("ALL") ? "" : reviewStage)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .build();

        System.out.println(searchRequest.toString());
        PagedDataResponse<LoanModel> response = getLoansUseCase.getPagedLoans(searchRequest, page, size);
        ApiResponseJSON<PagedDataResponse<LoanModel>> apiResponseJSON = new ApiResponseJSON<>("Loan transactions processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Secured("28") // CAN_VIEW_LOAN_RECORDS
    @ApiOperation(value = "Returns paginated list of loan customers.") //
    @GetMapping(value = "customers-profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<LoanCustomerProfileModel>>> getLoanCustomerProfiles(@ApiParam(value = "Verification Status: ALL, APPROVED, REJECTED, DECLINED, PENDING") @Valid @Pattern(regexp = "(ALL|APPROVED|REJECTED|PENDING|DECLINED)") @RequestParam(value = "verificationStatus", defaultValue = "ALL") String verificationStatus,
                                                                                                                @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                                                @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                                                                @RequestParam(value = "customerName", defaultValue = "", required = false) String customerName,
                                                                                                                @RequestParam(value = "customerPhone", defaultValue = "", required = false) String customerPhone,
                                                                                                                @ApiParam(value = "Review Stage: FIRST_REVIEW, SECOND_REVIEW") @Valid @Pattern(regexp = "(FIRST_REVIEW|SECOND_REVIEW|THIRD_REVIEW)") @RequestParam(value = "reviewStage", defaultValue = "FIRST_REVIEW") String reviewStage,
                                                                                                                @ApiParam(value = "No. of records per page. Min:1, Max:20") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
                                                                                                                @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {

        if(StringUtils.isNotEmpty(verificationStatus) && verificationStatus.equalsIgnoreCase("REJECTED")) {
            verificationStatus = "DECLINED";
        }
        if(StringUtils.isNotEmpty(customerPhone)){
            customerPhone = PhoneNumberUtils.toInternationalFormat(customerPhone);
        }
        CustomerProfileSearchRequest searchRequest = CustomerProfileSearchRequest.builder()
                .verificationStatus(verificationStatus)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .reviewStage(reviewStage)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        PagedDataResponse<LoanCustomerProfileModel> response = customerLoanProfileUseCase.getPagedLoanCustomerProfiles(searchRequest, page, size);
        ApiResponseJSON<PagedDataResponse<LoanCustomerProfileModel>> apiResponseJSON = new ApiResponseJSON<>("Loan customers processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns Employment Details of Customer Loan Profile.")
    @GetMapping(value = "customer-profile/{customerLoanProfileId}/employment-detail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> getLoanCustomerEmployerInfo(@PathVariable(value = "customerLoanProfileId") String profileId) {

        LoanCustomerProfileModel response = customerLoanProfileUseCase.getCustomerEmployerInfo(Long.parseLong(profileId));
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Employment details processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Data
    private static class LoanApprovalRequest {
        @ApiModelProperty(notes = "Reason for rejecting this loan")
        private String reason;

        @ApiModelProperty(notes = " true | True | false | False  |TRUE | FALSE", required = true)
        @NotEmpty
        @Pattern(regexp = "(true|True|false|False|TRUE|FALSE)")
        private String approved;
    }

    @Data
    private static class ProfileVerificationRequest {
        @ApiModelProperty(notes = "Reason for not verifying this profile")
        private String reason;

        @ApiModelProperty(notes = " true | True | false | False  |TRUE | FALSE", required = true)
        @NotEmpty
        @Pattern(regexp = "(true|True|false|False|TRUE|FALSE)")
        private String verified;
    }

    @Data
    private static class BlacklistRequest {
        @ApiModelProperty(notes = "Reason for blacklisting this user")
        private String reason;

        @ApiModelProperty(notes = " true | True | false | False  |TRUE | FALSE", required = true)
        @NotEmpty
        @Pattern(regexp = "(true|True|false|False|TRUE|FALSE)")
        private String blacklist;

    }

    @Data
    static class HNICustomerCreationRequestJSON {
        @Positive(message = "Interest rate must be a positive number")
        private double interestRate;

        private boolean chequeRequired;
        @ApiModelProperty(notes = " PRORATED_PRINCIPAL_INTEREST | END_OF_TENURE | INTEREST_ONLY", required = true)
        @NotEmpty
        @Pattern(regexp = "(PRORATED_PRINCIPAL_INTEREST|END_OF_TENURE|INTEREST_ONLY)")
        private String repaymentPlan;
        @NotEmpty
        @NotNull
        private String accountNumber;

        public HNICustomerCreationRequest toRequest() {
            return HNICustomerCreationRequest.builder()
                    .chequeRequired(chequeRequired)
                    .accountNumber(accountNumber)
                    .interestRate(interestRate)
                    .repaymentPlan(repaymentPlan)
                    .build();
        }
    }

}
