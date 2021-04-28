package com.mintfintech.savingsms.infrastructure.web.controllers.backoffice;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.features.loan.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.features.loan.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.features.loan.LoanApprovalUseCase;
import com.mintfintech.savingsms.usecase.data.request.CustomerProfileSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.LoanSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import com.mintfintech.savingsms.usecase.models.LoanTransactionModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
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

    @ApiOperation(value = "Verify Loan Customer Employment Information.")
    @PutMapping(value = "{customerLoanProfileId}/verify/employment-details", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> verifyEmploymentInformation(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                 @PathVariable(value = "customerLoanProfileId") String customerLoanProfileId,
                                                                                                 @RequestBody @Valid ProfileVerificationRequest request) {

        LoanCustomerProfileModel response = customerLoanProfileUseCase.verifyEmploymentInformation(authenticatedUser, Long.parseLong(customerLoanProfileId), Boolean.parseBoolean(request.getVerified()), request.getReason());
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Blacklist a customer.")
    @PutMapping(value = "{customerLoanProfileId}/blacklist", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> blackListCustomer(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                       @PathVariable(value = "customerLoanProfileId") String customerLoanProfileId,
                                                                                       @RequestBody @Valid BlacklistRequest request) {

        LoanCustomerProfileModel response = customerLoanProfileUseCase.blackListCustomer(authenticatedUser, Long.parseLong(customerLoanProfileId), Boolean.parseBoolean(request.getBlacklist()), request.getReason());
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Approve/Reject Loan Request.")
    @PostMapping(value = "{loanId}/approve", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanModel>> approveLoan(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                  @PathVariable(value = "loanId") String loanId,
                                                                  @RequestBody @Valid LoanApprovalRequest request) {

        LoanModel response = loanApprovalUseCase.approveLoanRequest(authenticatedUser, loanId, request.getReason(), Boolean.parseBoolean(request.getApproved()));
        ApiResponseJSON<LoanModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns list of loan transactions.")
    @GetMapping(value = "{loanId}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<LoanTransactionModel>>> getLoanTransactions(@PathVariable(value = "loanId") String loanId) {

        List<LoanTransactionModel> response = getLoansUseCase.getLoanTransactions(loanId);
        ApiResponseJSON<List<LoanTransactionModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns paginated loan list.")
    @GetMapping(value = "loans", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<LoanModel>>> getAllLoans(@ApiParam(value = "Repayment Status: ALL, PAID, PARTIALLY_PAID, PENDING, FAILED, CANCELLED") @Valid @Pattern(regexp = "(ALL|PAID|PARTIALLY_PAID|PENDING|FAILED|CANCELLED)") @RequestParam(value = "repaymentStatus", defaultValue = "ALL") String repaymentStatus,
                                                                                     @ApiParam(value = "Approval Status: ALL, APPROVED, DECLINED, PENDING, CANCELLED, DISBURSED") @Valid @Pattern(regexp = "(ALL|APPROVED|DECLINED|PENDING|CANCELLED|DISBURSED)") @RequestParam(value = "approvalStatus", defaultValue = "ALL") String approvalStatus,
                                                                                     @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                     @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                                     @ApiParam(value = "No. of records per page. Min:1, Max:20") @Valid @Min(value = 1) @Max(value = 20) @RequestParam("size") int size,
                                                                                     @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {

        LoanSearchRequest searchRequest = LoanSearchRequest.builder()
                .repaymentStatus(repaymentStatus)
                .fromDate(fromDate)
                .toDate(toDate)
                .approvalStatus(approvalStatus)
                .build();

        PagedDataResponse<LoanModel> response = getLoansUseCase.getPagedLoans(searchRequest, page, size);
        ApiResponseJSON<PagedDataResponse<LoanModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns paginated list of loan customers.")
    @GetMapping(value = "customers-profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<LoanCustomerProfileModel>>> getLoanCustomerProfiles(@ApiParam(value = "Verification Status: ALL, APPROVED, REJECTED, PENDING") @Valid @Pattern(regexp = "(ALL|APPROVED|REJECTED|PENDING)") @RequestParam(value = "verificationStatus", defaultValue = "ALL") String verificationStatus,
                                                                                                                @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                                                @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                                                                @ApiParam(value = "No. of records per page. Min:1, Max:20") @Valid @Min(value = 1) @Max(value = 20) @RequestParam("size") int size,
                                                                                                                @ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page
    ) {

        CustomerProfileSearchRequest searchRequest = CustomerProfileSearchRequest.builder()
                .verificationStatus(verificationStatus)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        PagedDataResponse<LoanCustomerProfileModel> response = customerLoanProfileUseCase.getPagedLoanCustomerProfiles(searchRequest, page, size);
        ApiResponseJSON<PagedDataResponse<LoanCustomerProfileModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns Employment Details of Customer Loan Profile.")
    @GetMapping(value = "customer-profile/{customerLoanProfileId}/employment-detail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> getLoanCustomerEmployerInfo(@PathVariable(value = "customerLoanProfileId") String profileId) {

        LoanCustomerProfileModel response = customerLoanProfileUseCase.getCustomerEmployerInfo(Long.parseLong(profileId));
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
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

}
