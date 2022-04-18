package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.usecase.GetCustomerSavingsDataUseCase;
import com.mintfintech.savingsms.usecase.data.response.CustomerSavingsStatisticResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Sun, 17 Apr, 2022
 */
@Slf4j
@FieldDefaults(makeFinal = true)
@Api(tags = "Intranet Service Endpoints", description = "Services for intra microservice service consumption")
@RestController
@RequestMapping(value = "/api/v1/intranet", headers = {"Authorization"})
@AllArgsConstructor
public class IntranetController {

    private final GetCustomerSavingsDataUseCase getCustomerSavingsDataUseCase;

    @ApiOperation(value = "Returns a summary funding ")
    @GetMapping(value = "/savings-summary/{accountId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<CustomerSavingsStatisticResponse>> getSavingsSummary(@PathVariable("accountId") String accountId,
                                                                                               @ApiParam(value="Format: dd/MM/yyyy") @DateTimeFormat(pattern="dd/MM/yyyy") @RequestParam(value = "fromDate") LocalDate fromDate,
                                                                                               @RequestHeader(required = false, name = "x-request-client-key") String clientKey) {
        if(StringUtils.isNotEmpty(clientKey)) {
            System.out.println("clientKey - "+clientKey);
            // Internal MS request does not carry client-key
            //throw new UnauthorisedException("Unauthorised request.");
        }
        CustomerSavingsStatisticResponse response = getCustomerSavingsDataUseCase.getCustomerSavingsStatistics(accountId);
        ApiResponseJSON<CustomerSavingsStatisticResponse> apiResponseJSON = new ApiResponseJSON<>("Request processed successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
