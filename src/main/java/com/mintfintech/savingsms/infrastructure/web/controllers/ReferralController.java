package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.ReferralDetailsResponse;
import com.mintfintech.savingsms.usecase.features.referral_savings.GetReferralRewardUseCase;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "Referral Endpoint(s)", description = "Handles the referrals.")
@RestController
@RequestMapping(value = "/api/v1/referral", headers = {"x-request-client-key", "Authorization"})
@AllArgsConstructor
public class ReferralController {

    private GetReferralRewardUseCase getReferralRewardUseCase;

    @ApiOperation(value = "Returns details of a referrer.")
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<ReferralDetailsResponse>> getReferralDetails(@ApiIgnore @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        ReferralDetailsResponse response = getReferralRewardUseCase.getReferralDetails(authenticatedUser);
        ApiResponseJSON<ReferralDetailsResponse> apiResponseJSON = new ApiResponseJSON<>("Request processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
