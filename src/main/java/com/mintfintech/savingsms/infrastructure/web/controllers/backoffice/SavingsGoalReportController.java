package com.mintfintech.savingsms.infrastructure.web.controllers.backoffice;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.backoffice.GetSavingsTransactionUseCase;
import com.mintfintech.savingsms.usecase.data.request.InvestmentTransactionSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.SavingsSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentTransactionSearchResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.PortalSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.data.response.SavingsMaturityStatSummary;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Created by jnwanya on Sat, 06 Jun, 2020
 */
//@Secured("ADMIN_PORTAL")
//@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Api(tags = "Savings Goal Report Endpoints (BO)", description = "Back-office: Handles savings goal report management.")
@RestController
@RequestMapping(value = "/api/v1/admin/", headers = { "x-request-client-key", "Authorization" })
@AllArgsConstructor
public class SavingsGoalReportController {

	GetSavingsGoalUseCase getSavingsGoalUseCase;
	GetSavingsTransactionUseCase getSavingsTransactionUseCase;

	@Secured("39") // Privilege: CAN_VIEW_SAVINGS_TRANSACTION
	@ApiOperation(value = "Returns paginated list of savings goal.")
	@GetMapping(value = "savings-goals", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<PagedDataResponse<PortalSavingsGoalResponse>>> getSavingsGoal(
			@RequestParam(value = "accountId", required = false) String accountId,
			@RequestParam(value = "customerName", required = false) String customerName,
			@RequestParam(value = "goalName", required = false) String goalName,
			@NotBlank @Pattern(regexp = "(ACTIVE|MATURED|COMPLETED)") @RequestParam(value = "goalStatus", defaultValue = "ACTIVE") String goalStatus,
			@NotBlank @Pattern(regexp = "(ALL|ENABLED|DISABLED)") @RequestParam(value = "autoSaveStatus", defaultValue = "ALL") String autoSaveStatus,
			@Pattern(regexp = "(ALL|ROUND_UP_SAVINGS|CUSTOMER_SAVINGS|EMERGENCY_SAVINGS|MINT_REFERRAL_EARNINGS)") @RequestParam(value = "savingsType", required = false, defaultValue = "ALL") String savingsType,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
			@RequestParam("size") int size, @RequestParam("page") int page) {
		SavingsSearchRequest searchRequest = SavingsSearchRequest.builder().customerName(customerName)
				.savingsStatus(goalStatus).goalName(goalName).accountId(accountId).savingsType(savingsType)
				.fromDate(fromDate).toDate(toDate).autoSavedStatus(autoSaveStatus).build();
		PagedDataResponse<PortalSavingsGoalResponse> response = getSavingsGoalUseCase
				.getPagedSavingsGoals(searchRequest, page, size);
		ApiResponseJSON<PagedDataResponse<PortalSavingsGoalResponse>> apiResponseJSON = new ApiResponseJSON<>(
				"Savings goals processed successfully.", response);
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

	@Secured("06") // Privilege: CAN_VIEW_CUSTOMER_INFORMATION
	@ApiOperation(value = "Returns paginated list of customer savings goal using accountId.")
	@GetMapping(value = "customer/{accountId}/savings-goal", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<PagedDataResponse<PortalSavingsGoalResponse>>> getCustomerSavingsGoalByAccountId(
			@PathVariable(value = "accountId") String accountId,
			@NotBlank @Pattern(regexp = "(ACTIVE|MATURED|COMPLETED)") @RequestParam(value = "goalStatus", defaultValue = "ALL") String goalStatus,
			@NotBlank @Pattern(regexp = "(ALL|ENABLED|DISABLED)") @RequestParam(value = "autoSaveStatus", defaultValue = "ALL") String autoSaveStatus,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate", required = false) LocalDate toDate,
			@Valid @Min(value = 1) @Max(value = 500) @RequestParam("size") int size,
			@Valid @Min(value = 0) @RequestParam("page") int page) {

		SavingsSearchRequest searchRequest = SavingsSearchRequest.builder()
				.savingsStatus(goalStatus.equalsIgnoreCase("ALL") ? "" : goalStatus).accountId(accountId)
				.fromDate(fromDate).toDate(toDate).autoSavedStatus(autoSaveStatus).build();
		PagedDataResponse<PortalSavingsGoalResponse> response = getSavingsGoalUseCase
				.getPagedSavingsGoals(searchRequest, page, size);
		ApiResponseJSON<PagedDataResponse<PortalSavingsGoalResponse>> apiResponseJSON = new ApiResponseJSON<>(
				"Savings goal processed successfully.", response);
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

	@Secured("08") // Privilege: VIEW_TRANSACTION_REPORTS
	@ApiOperation(value = "Returns savings goal details by goal id.")
	@GetMapping(value = "savings-goals/{goalId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<PortalSavingsGoalResponse>> getSavingsGoal(
			@PathVariable("goalId") String goalId) {

		PortalSavingsGoalResponse response = getSavingsGoalUseCase.getPortalSavingsGoalResponseByGoalId(goalId);
		ApiResponseJSON<PortalSavingsGoalResponse> apiResponseJSON = new ApiResponseJSON<>(
				"Request processed successfully.", response);
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

	@Secured("09") // Privilege: VIEW_DASHBOARD_STATISTICS
	@ApiOperation(value = "Returns savings maturity statistics information.")
	@GetMapping(value = "savings-goals/maturity-statistics", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponseJSON<SavingsMaturityStatSummary>> getSavingsMaturityStatistics(
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "fromDate") LocalDate fromDate,
			@ApiParam(value = "Format: dd/MM/yyyy") @DateTimeFormat(pattern = "dd/MM/yyyy") @RequestParam(value = "toDate") LocalDate toDate) {
		SavingsMaturityStatSummary response = getSavingsTransactionUseCase.getSavingsMaturityStatistics(fromDate,
				toDate);
		ApiResponseJSON<SavingsMaturityStatSummary> apiResponseJSON = new ApiResponseJSON<>(
				"Maturity processed successfully.", response);
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}

//	@Secured("08") // Privilege: VIEW_TRANSACTION_REPORTS
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
				.transactionAmount((transactionAmount != null && StringUtils.isNotEmpty(transactionAmount))
						? new BigDecimal(transactionAmount)
						: null)
				.build();
		if (fromDate != null && toDate != null) {
			searchRequest.setFromDate(fromDate.atStartOfDay());
			searchRequest.setToDate(toDate.atTime(23, 59, 59));
		} else {
			searchRequest.setFromDate(null);
			searchRequest.setToDate(null);
		}
		PagedDataResponse<InvestmentTransactionSearchResponse> response = getSavingsTransactionUseCase
				.getInvestmentTransactions(searchRequest, page, size);
		ApiResponseJSON<PagedDataResponse<InvestmentTransactionSearchResponse>> apiResponseJSON = new ApiResponseJSON<>(
				"Transactions returned successfully.", response);
		return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
	}
}
