package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.usecase.features.referral_savings.CreateReferralRewardUseCase;
import com.mintfintech.savingsms.usecase.features.referral_savings.impl.CreateReferralRewardUseCaseImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */

@RestController
public class IndexController {

    private CreateReferralRewardUseCase createReferralRewardUseCase;
    @Autowired
    public void setCreateReferralRewardUseCase(CreateReferralRewardUseCase createReferralRewardUseCase) {
        this.createReferralRewardUseCase = createReferralRewardUseCase;
    }

    @GetMapping(value = {""}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> indexPage() {
        ApiResponseJSON<Object> apiResponse = new ApiResponseJSON<>("Confirmed, Savings  Service is up and running.");
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    /*
    @GetMapping(value = "/timeout-test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> timeoutStressTest(@RequestParam(value = "seconds", defaultValue = "60", required = false) int seconds) {
        long delayMilliSeconds = seconds * 1000;
        try{
            Thread.sleep(delayMilliSeconds);
        }catch (Exception ignored) {}
        ApiResponseJSON<Object> apiResponse = new ApiResponseJSON<>("No timeout after "+seconds+" seconds.");
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
    */

    @GetMapping(value = "/referral-reward", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> referralReward(@RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                                  @RequestParam(value = "userId", defaultValue = "", required = false) String userId,
                                                                  @RequestParam(value = "overridePeriod", defaultValue = "false", required = false) boolean overridePeriod) {
        new Thread(() -> {
            createReferralRewardUseCase.processReferralByUser(userId, size, overridePeriod);
        }).start();
        ApiResponseJSON<Object> apiResponse = new ApiResponseJSON<>("Processed reward");
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
