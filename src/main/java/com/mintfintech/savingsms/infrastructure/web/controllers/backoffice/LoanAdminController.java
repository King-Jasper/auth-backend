package com.mintfintech.savingsms.infrastructure.web.controllers.backoffice;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.LoanUseCase;
import com.mintfintech.savingsms.usecase.data.request.CustomerProfileSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.LoanSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import io.swagger.annotations.Api;
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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

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
    private final LoanUseCase loanUseCase;

    @ApiOperation(value = "Verify Loan Customer Employment Information.")
    @PutMapping(value = "{customerLoanProfileId}/verify/employment-details", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> verifyEmploymentInformation(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                                 @PathVariable("customerLoanProfileId") String customerLoanProfileId,
                                                                                                 @RequestBody ProfileVerificationRequest request) {

        LoanCustomerProfileModel response = customerLoanProfileUseCase.verifyEmploymentInformation(authenticatedUser, Long.parseLong(customerLoanProfileId), request.isVerified(), request.getReason());
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Blacklist a customer.")
    @PutMapping(value = "{customerLoanProfileId}/blacklist", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> blackListCustomer(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                                       @PathVariable("customerLoanProfileId") String customerLoanProfileId,
                                                                                       @RequestBody BlacklistRequest request) {

        LoanCustomerProfileModel response = customerLoanProfileUseCase.blackListCustomer(authenticatedUser, Long.parseLong(customerLoanProfileId), request.isBlacklist(), request.getReason());
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Approve/Reject Loan Request.")
    @PostMapping(value = "{loanId}/approve", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanModel>> approveLoan(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                  @PathVariable("loanId") String loanId,
                                                                  @RequestBody @Valid LoanApprovalRequest request) {

        LoanModel response = loanUseCase.approveLoanRequest(authenticatedUser, loanId, request.getReason(), request.approved);
        ApiResponseJSON<LoanModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns paginated loan list.")
    @GetMapping(value = "loans", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<LoanModel>>> getAllLoans(@Pattern(regexp = "(PAID|PARTIALLY_PAID|PENDING|FAILED)") @RequestParam("loanStatus") String loanStatus,
                                                                                     @Pattern(regexp = "(APPROVED|REJECTED|PENDING|CANCELLED)") @RequestParam("approvalStatus") String approvalStatus,
                                                                                     @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                     @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                                     @RequestParam("size") int size,
                                                                                     @RequestParam("page") int page
    ) {

        LoanSearchRequest searchRequest = LoanSearchRequest.builder()
                .loanStatus(loanStatus)
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
    public ResponseEntity<ApiResponseJSON<PagedDataResponse<LoanCustomerProfileModel>>> getLoanCustomerProfiles(@Pattern(regexp = "(APPROVED|REJECTED|PENDING)") @RequestParam("verificationStatus") String verificationStatus,
                                                                                                                @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                                                                                @ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                                                                                @NotNull @RequestParam("size") int size,
                                                                                                                @NotNull @RequestParam("page") int page
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
    public ResponseEntity<ApiResponseJSON<LoanCustomerProfileModel>> getLoanCustomerEmployerInfo(@PathVariable("customerLoanProfileId") String profileId) {

        LoanCustomerProfileModel response = customerLoanProfileUseCase.getCustomerEmployerInfo(Long.parseLong(profileId));
        ApiResponseJSON<LoanCustomerProfileModel> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @Data
    private static class LoanApprovalRequest {
        private String reason;

        @NotNull
        private boolean approved;
    }

    @Data
    private static class ProfileVerificationRequest {
        private String reason;

        @NotNull
        private boolean verified;
    }

    @Data
    private static class BlacklistRequest {
        private String reason;

        @NotNull
        private boolean blacklist;

    }

}
