package com.mintfintech.savingsms.infrastructure.web.controllers.backoffice;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.models.FundInvestmentByAdminJSON;
import com.mintfintech.savingsms.infrastructure.web.models.InvestmentCreationAdminRequestJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentTransactionSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentCreationResponse;
import com.mintfintech.savingsms.usecase.data.response.InvestmentFundingResponse;
import com.mintfintech.savingsms.usecase.data.response.InvestmentMaturityStatSummary;
import com.mintfintech.savingsms.usecase.data.response.InvestmentStatSummary;
import com.mintfintech.savingsms.usecase.data.response.InvestmentTransactionSearchResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.features.investment.CreateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.FundInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import springfox.documentation.annotations.ApiIgnore;

@Secured("ADMIN_PORTAL")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Api(tags = "Investment Admin Endpoints")
@RestController
@RequestMapping(value = "/api/v1/admin/investment", headers = { "x-request-client-key", "Authorization" })
@RequiredArgsConstructor
public class InvestmentAdminController {

	private final GetInvestmentUseCase getInvestmentUseCase;
	private final CreateInvestmentUseCase createInvestmentUseCase;
	private final FundInvestmentUseCase fundInvestmentUseCase;

	@Secured("09") // Privilege: VIEW_DASHBOARD_STATISTICS
	@ApiOperation(value = "Returns investment maturity statistics information.")
	@GetMapping(value = "/maturity-statistics", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<InvestmentMaturityStatSummary>> getSavingsMaturityStatistics(
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate) {
		if (fromDate == null || toDate == null) {
			fromDate = LocalDate.now();
			toDate = fromDate.plusWeeks(1); // default
		}
		InvestmentMaturityStatSummary response = getInvestmentUseCase.getMaturityStatistics(fromDate, toDate);
		ApiResponseJSON<InvestmentMaturityStatSummary> apiResponseJSON = new ApiResponseJSON<>(
				"Investment maturity statistics processed successfully.", response);
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

	@Secured("19") // Privilege: CAN_VIEW_INVESTMENT
	@ApiOperation(value = "Returns paginated investment list.")
	@GetMapping(value = "/completed", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<PagedDataResponse<InvestmentModel>>> getCompletedInvestments(
			@ApiParam(value = "Investment Status: ALL, COMPLETED, LIQUIDATED") @Valid @Pattern(regexp = "(ALL|COMPLETED|LIQUIDATED)") @RequestParam(value = "investmentStatus", defaultValue = "ALL") String investmentStatus,
			@ApiParam(value = "Customer first or last name") @RequestParam(value = "customerName", required = false) String customerName,
			@ApiParam(value = "Account Type: INDIVIDUAL|CORPORATE") @RequestParam(value = "accountType", required = false) String accountType,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startFromDate", required = false) LocalDate startFromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startToDate", required = false) LocalDate startToDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureFromDate", required = false) LocalDate matureFromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureToDate", required = false) LocalDate matureToDate,
			@ApiParam(value = "Duration of investment in months. Min:1") @Valid @Min(value = 0) @RequestParam(value = "duration", required = false, defaultValue = "0") int duration,
			@ApiParam(value = "No. of records per page. Min:1, Max:500") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
			@ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page) {

		InvestmentSearchRequest searchRequest = InvestmentSearchRequest.builder()
				.investmentStatus(investmentStatus.equalsIgnoreCase("ALL") ? "" : investmentStatus)
				.startToDate(startToDate)
				.customerName(StringUtils.isNotEmpty(customerName) ? customerName.trim() : null).duration(duration)
				.matureFromDate(matureFromDate).matureToDate(matureToDate).startFromDate(startFromDate)
				.completedRecords(true).accountType(accountType).build();

		InvestmentStatSummary response = getInvestmentUseCase.getPagedInvestments(searchRequest, page, size);

		ApiResponseJSON<PagedDataResponse<InvestmentModel>> apiResponseJSON = new ApiResponseJSON<>(
				"Investment list returned successfully.", response.getInvestments());
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

	@Secured("19") // Privilege: CAN_VIEW_INVESTMENT
	@ApiOperation(value = "Returns paginated investment list.")
	@GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<PagedDataResponse<InvestmentModel>>> getAllInvestments(
			@ApiParam(value = "Customer first or last name") @RequestParam(value = "customerName", required = false) String customerName,
			@ApiParam(value = "Account Type: INDIVIDUAL|CORPORATE") @RequestParam(value = "accountType", required = false) String accountType,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startFromDate", required = false) LocalDate startFromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startToDate", required = false) LocalDate startToDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureFromDate", required = false) LocalDate matureFromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureToDate", required = false) LocalDate matureToDate,
			@ApiParam(value = "Duration of investment in months. Min:1") @Valid @Min(value = 0) @RequestParam(value = "duration", required = false, defaultValue = "0") int duration,
			@ApiParam(value = "No. of records per page. Min:1, Max:500") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
			@ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page) {

		InvestmentSearchRequest searchRequest = InvestmentSearchRequest.builder().startToDate(startToDate)
				.customerName(StringUtils.isNotEmpty(customerName) ? customerName.trim() : null).duration(duration)
				.matureFromDate(matureFromDate).matureToDate(matureToDate).startFromDate(startFromDate)
				.investmentStatus("ACTIVE").completedRecords(false).accountType(accountType).build();

		InvestmentStatSummary response = getInvestmentUseCase.getPagedInvestments(searchRequest, page, size);

		ApiResponseJSON<PagedDataResponse<InvestmentModel>> apiResponseJSON = new ApiResponseJSON<>(
				"Investment list returned successfully.", response.getInvestments());
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

	@Secured("06") // Privilege: CAN_VIEW_CUSTOMER_INVESTMENT
	@ApiOperation(value = "Returns paginated investment list.")
	@GetMapping(value = "/customer/{accountId}/completed", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<PagedDataResponse<InvestmentModel>>> getCustomerCompletedInvestments(
			@PathVariable(value = "accountId") String accountId,
			@ApiParam(value = "Investment Status: ALL, COMPLETED, LIQUIDATED") @Valid @Pattern(regexp = "(ALL|COMPLETED|LIQUIDATED)") @RequestParam(value = "investmentStatus", defaultValue = "ALL") String investmentStatus,
			@ApiParam(value = "Customer first or last name") @RequestParam(value = "customerName", required = false) String customerName,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startFromDate", required = false) LocalDate startFromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startToDate", required = false) LocalDate startToDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureFromDate", required = false) LocalDate matureFromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureToDate", required = false) LocalDate matureToDate,
			@ApiParam(value = "Duration of investment in months. Min:1") @Valid @Min(value = 0) @RequestParam(value = "duration", required = false, defaultValue = "0") int duration,
			@ApiParam(value = "No. of records per page. Min:1, Max:500") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
			@ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page) {

		InvestmentSearchRequest searchRequest = InvestmentSearchRequest.builder().accountId(accountId)
				.investmentStatus(investmentStatus.equalsIgnoreCase("ALL") ? "" : investmentStatus)
				.startToDate(startToDate)
				.customerName(StringUtils.isNotEmpty(customerName) ? customerName.trim() : null).duration(duration)
				.matureFromDate(matureFromDate).matureToDate(matureToDate).startFromDate(startFromDate)
				.completedRecords(true).build();

		InvestmentStatSummary response = getInvestmentUseCase.getPagedInvestmentsByAdmin(searchRequest, page, size);

		ApiResponseJSON<PagedDataResponse<InvestmentModel>> apiResponseJSON = new ApiResponseJSON<>(
				"Investment list returned successfully.", response.getInvestments());
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

	@Secured("06") // Privilege: CAN_VIEW_CUSTOMER_INVESTMENT
	@ApiOperation(value = "Returns paginated investment list.")
	@GetMapping(value = "/customer/{accountId}/active", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<PagedDataResponse<InvestmentModel>>> getCustomerActiveInvestments(
			@PathVariable(value = "accountId") String accountId,
			@ApiParam(value = "Customer first or last name") @RequestParam(value = "customerName", required = false) String customerName,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startFromDate", required = false) LocalDate startFromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "startToDate", required = false) LocalDate startToDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureFromDate", required = false) LocalDate matureFromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "matureToDate", required = false) LocalDate matureToDate,
			@ApiParam(value = "Duration of investment in months. Min:1") @Valid @Min(value = 0) @RequestParam(value = "duration", required = false, defaultValue = "0") int duration,
			@ApiParam(value = "No. of records per page. Min:1, Max:500") @Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
			@ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam("page") int page) {

		InvestmentSearchRequest searchRequest = InvestmentSearchRequest.builder().accountId(accountId)
				.investmentStatus("ACTIVE").startToDate(startToDate)
				.customerName(StringUtils.isNotEmpty(customerName) ? customerName.trim() : null).duration(duration)
				.matureFromDate(matureFromDate).matureToDate(matureToDate).startFromDate(startFromDate)
				.completedRecords(false).build();

		InvestmentStatSummary response = getInvestmentUseCase.getPagedInvestmentsByAdmin(searchRequest, page, size);

		ApiResponseJSON<PagedDataResponse<InvestmentModel>> apiResponseJSON = new ApiResponseJSON<>(
				"Investment list returned successfully.", response.getInvestments());
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

	@Secured("20") // Privilege: CAN_CREATE_INVESTMENT
	@ApiOperation(value = "Creates a new investment.")
	@PostMapping(value = "/customer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<InvestmentCreationResponse>> createInvestment(
			@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@RequestBody @Valid InvestmentCreationAdminRequestJSON requestJSON) {
		InvestmentCreationResponse response = createInvestmentUseCase.createInvestmentByAdmin(authenticatedUser,
				requestJSON.toRequest());
		ApiResponseJSON<InvestmentCreationResponse> apiResponseJSON = new ApiResponseJSON<>(
				"Investment created successfully.", response);
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

	@Secured("20")
	@ApiOperation(value = "Fund an investment.")
	@PostMapping(value = "/customer/fund", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<InvestmentFundingResponse>> fundInvestment(
			@RequestBody @Valid FundInvestmentByAdminJSON requestJSON) {
		InvestmentFundingResponse response = fundInvestmentUseCase.fundInvestmentByAdmin(requestJSON.toRequest());
		ApiResponseJSON<InvestmentFundingResponse> apiResponseJSON = new ApiResponseJSON<>(
				"Investment funded successfully.", response);
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

	@Secured("08") // Privilege: VIEW_TRANSACTION_REPORTS
	@ApiOperation(value = "Returns paginated list of investment transactions.")
	@GetMapping(value = "/investment-transaction/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<PagedDataResponse<InvestmentTransactionSearchResponse>>> getCardlessWithdrawals(
			@ApiParam(value = "Sender's account number") @RequestParam(value = "mintAccountNumber", required = false) String mintAccountNumber,
			@ApiParam(value = "Transaction Reference") @RequestParam(value = "transactionReference", required = false) String transactionReference,
			@ApiParam(value = "Transaction status: PENDING, FAILED, CANCELLED, SUCCESSFUL, pending,failed,cancelled,successful") @Pattern(regexp = "^SUCCESSFUL|FAILED|PENDING|CANCELLED|successful|failed|pending|cancelled$") @RequestParam(value = "transactionStatus", required = false, defaultValue = "SUCCESSFUL") String transactionStatus,
			@ApiParam(value = "Transaction Type: CREDIT, DEBIT,credit,debit") @Pattern(regexp = "^CREDIT|DEBIT|credit|debit$") @RequestParam(value = "transactionType", required = false, defaultValue = "CREDIT") String transactionType,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
			@ApiParam(value = "No. of records per page. Min:1, Max:20") @Valid @Min(value = 1) @Max(value = 500) @RequestParam(value = "size", defaultValue = "20") int size,
			@ApiParam(value = "The index of the page to return. Min: 0") @Valid @Min(value = 0) @RequestParam(value = "page", defaultValue = "1") int page,
			@ApiParam(value = "Transaction amount, Format: 0.00 (Decimal numbers)") @Pattern(regexp = "^\\d*\\.?\\d+$", message = "invalid input for amount") @RequestParam(value = "transactionAmount", required = false, defaultValue = "0.00") String transactionAmount) {

		InvestmentTransactionSearchRequest searchRequest = InvestmentTransactionSearchRequest.builder()
				.transactionStatus(
						(transactionStatus != null) ? TransactionStatusConstant.valueOf(transactionStatus.toUpperCase())
								: null)
				.transactionType(
						(transactionType != null) ? TransactionTypeConstant.valueOf(transactionType.toUpperCase())
								: null)
				.mintAccountNumber(mintAccountNumber).transactionReference(transactionReference)
				.transactionAmount(
						StringUtils.isNotEmpty(transactionAmount) ? new BigDecimal(transactionAmount) : BigDecimal.ZERO)
				.build();
		if (fromDate != null && toDate != null) {
			searchRequest.setFromDate(fromDate.atStartOfDay());
			searchRequest.setToDate(toDate.atTime(23, 59, 59));
		} else {
			searchRequest.setFromDate(null);
			searchRequest.setToDate(null);
		}
		PagedDataResponse<InvestmentTransactionSearchResponse> response = getInvestmentUseCase
				.getInvestmentTransactions(searchRequest, page, size);
		ApiResponseJSON<PagedDataResponse<InvestmentTransactionSearchResponse>> apiResponseJSON = new ApiResponseJSON<>(
				"Transactions returned successfully.", response);
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

}
