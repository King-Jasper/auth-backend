package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.usecase.master_record.SavingsGoalCategoryUseCase;
import com.mintfintech.savingsms.usecase.master_record.SavingsPlanUseCases;
import com.mintfintech.savingsms.usecase.models.SavingsGoalCategoryModel;
import com.mintfintech.savingsms.usecase.models.SavingsPlanModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Api(tags = "Master Record Endpoints", description = "Handles the static records of the application.")
@RestController
@RequestMapping(value = "/api/v1/common", headers = {"x-request-client-key"})
public class MasterRecordsController {

    private SavingsPlanUseCases savingsPlanUseCases;
    private SavingsGoalCategoryUseCase savingsGoalCategoryUseCase;

    public MasterRecordsController(SavingsPlanUseCases savingsPlanUseCases, SavingsGoalCategoryUseCase savingsGoalCategoryUseCase) {
        this.savingsPlanUseCases = savingsPlanUseCases;
        this.savingsGoalCategoryUseCase = savingsGoalCategoryUseCase;
    }

    @ApiOperation(value = "Returns a list of saving plans.")
    @GetMapping(value = "/savings-plans", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsPlanModel>>> getSavingsPlanList() {
        List<SavingsPlanModel> responseList = savingsPlanUseCases.savingsPlanList();
        ApiResponseJSON<List<SavingsPlanModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of saving plans.")
    @GetMapping(value = "/savings-goal-categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsGoalCategoryModel>>> getSavingsGoalCategoryList() {
        List<SavingsGoalCategoryModel> responseList = savingsGoalCategoryUseCase.savingsGoalCategoryList();
        ApiResponseJSON<List<SavingsGoalCategoryModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
