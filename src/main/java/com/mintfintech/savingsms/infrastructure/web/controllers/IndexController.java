package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */

@RestController
public class IndexController {

    @GetMapping(value = {""}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<Object>> indexPage() {
        ApiResponseJSON<Object> apiResponse = new ApiResponseJSON<>("Confirmed, Savings  Service is up and running.");
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
