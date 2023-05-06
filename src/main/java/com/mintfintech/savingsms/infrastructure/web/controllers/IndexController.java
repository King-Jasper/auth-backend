package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.domain.models.corebankingservice.LoanDetailResponseCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.usecase.ApplySavingsInterestUseCase;
import com.mintfintech.savingsms.usecase.CreateSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.response.InterestUpdateResponse;
import com.mintfintech.savingsms.usecase.features.referral_savings.CreateReferralRewardUseCase;
import com.mintfintech.savingsms.usecase.features.referral_savings.ReachHQTransactionUseCase;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */

@RestController
public class IndexController {

    private CreateReferralRewardUseCase createReferralRewardUseCase;
    private CreateSavingsGoalUseCase createSavingsGoalUseCase;
    private CoreBankingServiceClient coreBankingServiceClient;
    private ApplySavingsInterestUseCase applySavingsInterestUseCase;
    private SystemIssueLogService systemIssueLogService;


    @Autowired
    public void setCreateSavingsGoalUseCase(CreateSavingsGoalUseCase createSavingsGoalUseCase) {
        this.createSavingsGoalUseCase = createSavingsGoalUseCase;
    }
    @Autowired
    public void setCreateReferralRewardUseCase(CreateReferralRewardUseCase createReferralRewardUseCase) {
        this.createReferralRewardUseCase = createReferralRewardUseCase;
    }
    @Autowired
    public void setCoreBankingServiceClient(CoreBankingServiceClient coreBankingServiceClient) {
        this.coreBankingServiceClient = coreBankingServiceClient;
    }

    @Autowired
    public void setApplySavingsInterestUseCase(ApplySavingsInterestUseCase applySavingsInterestUseCase) {
        this.applySavingsInterestUseCase = applySavingsInterestUseCase;
    }

    @Autowired
    public void setSystemIssueLogService(SystemIssueLogService systemIssueLogService) {
        this.systemIssueLogService = systemIssueLogService;
    }

    @GetMapping(value = {""}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> indexPage() {
        ApiResponseJSON<Object> apiResponse = new ApiResponseJSON<>("Confirmed, Savings  Service is up and running.");
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }


    @GetMapping(value = "/timeout-test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> timeoutStressTest(@RequestParam(value = "seconds", defaultValue = "60", required = false) int seconds) {
        long delayMilliSeconds = seconds * 1000;
        try{
            Thread.sleep(delayMilliSeconds);
        }catch (Exception ignored) {}
        systemIssueLogService.logIssue("TIMEOUT TEST COMPLETED", "TIMEOUT TEST COMPLETED IN "+seconds+" seconds", "");
        ApiResponseJSON<Object> apiResponse = new ApiResponseJSON<>("No timeout after "+seconds+" seconds.");
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping(value = "/referral-reward", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> referralReward(@RequestParam(value = "userId", defaultValue = "", required = false) String userId,
                                                                  @RequestParam(value = "phoneNumber", defaultValue = "", required = false) String phoneNumber) {
        String response = createReferralRewardUseCase.processReferralByUser(userId, phoneNumber);
        ApiResponseJSON<Object> apiResponse = new ApiResponseJSON<>(response);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping(value = "/referral-reward-old", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> referralReward(@RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                                  @RequestParam(value = "userId", defaultValue = "", required = false) String userId,
                                                                  @RequestParam(value = "phoneNumber", defaultValue = "", required = false) String phoneNumber,
                                                                  @RequestParam(value = "overridePeriod", defaultValue = "false", required = false) boolean overridePeriod) {
        String response = createReferralRewardUseCase.processReferralByUserOld(userId, phoneNumber, size, overridePeriod);
        ApiResponseJSON<Object> apiResponse = new ApiResponseJSON<>(response);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    /*
    @GetMapping(value = "/referral-backlog-reward", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> referralReward(@RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                                  @ApiParam(value="Format: dd/MM/yyyy")  @DateTimeFormat(pattern="dd/MM/yyyy HH:mm") @RequestParam(value = "fromDate", required = false) LocalDateTime fromDate,
                                                                  @ApiParam(value="Format: dd/MM/yyyy")  @DateTimeFormat(pattern="dd/MM/yyyy HH:mm") @RequestParam(value = "toDate", required = false) LocalDateTime toDate) {
        createReferralRewardUseCase.processReferralBackLog(fromDate, toDate, size);
        ApiResponseJSON<Object> apiResponse = new ApiResponseJSON<>("Processed reward");
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }*/

    @GetMapping(value = "/interest-update", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> interestUpdate() {
        createSavingsGoalUseCase.runInterestUpdate();
        ApiResponseJSON<Object> apiResponse = new ApiResponseJSON<>("Process initiated successfully.");
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping(value = "/savings-interest-update", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<InterestUpdateResponse>> processSavingsInterestUpdate(@RequestParam("goalId") String goalId, @RequestParam("applyInterest") boolean applyInterest) {
        InterestUpdateResponse response = applySavingsInterestUseCase.recalculateInterestOnSavings(goalId, applyInterest);
        ApiResponseJSON<InterestUpdateResponse> apiResponse = new ApiResponseJSON<>("Processed", response);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping(value = "/loan-details", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<LoanDetailResponseCBS>> fetchLoanDetails(@RequestParam("accountNumber") String accountNo, @RequestParam("customerId") String customerId) {
        MsClientResponse<LoanDetailResponseCBS> responseMs =  coreBankingServiceClient.getLoanDetails(customerId, accountNo);
        ApiResponseJSON<LoanDetailResponseCBS> apiResponse = new ApiResponseJSON<>("Success.", responseMs.getData());
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
