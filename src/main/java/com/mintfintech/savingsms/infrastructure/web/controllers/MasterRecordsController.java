package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.usecase.master_record.SavingsGoalCategoryUseCase;
import com.mintfintech.savingsms.usecase.master_record.SavingsPlanUseCases;
import com.mintfintech.savingsms.usecase.models.SavingsGoalCategoryModel;
import com.mintfintech.savingsms.usecase.models.SavingsPlanModel;
import com.mintfintech.savingsms.usecase.models.SavingsPlanTenorModel;
import com.mintfintech.savingsms.usecase.models.deprecated.SavingsPlanDModel;
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
@RequestMapping(value = "/api", headers = {"x-request-client-key"})
public class MasterRecordsController {

    private final SavingsPlanUseCases savingsPlanUseCases;
    private final SavingsGoalCategoryUseCase savingsGoalCategoryUseCase;

    public MasterRecordsController(SavingsPlanUseCases savingsPlanUseCases, SavingsGoalCategoryUseCase savingsGoalCategoryUseCase) {
        this.savingsPlanUseCases = savingsPlanUseCases;
        this.savingsGoalCategoryUseCase = savingsGoalCategoryUseCase;
    }

    @Deprecated
    @ApiOperation(value = "Returns a list of saving plans.")
    @GetMapping(value = "/v1/common/savings-plans", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsPlanDModel>>> getSavingsPlanDeprecatedList() {
        List<SavingsPlanDModel> responseList = savingsPlanUseCases.savingsPlanDeprecatedList();
        ApiResponseJSON<List<SavingsPlanDModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of saving plans.")
    @GetMapping(value = "/v2/common/savings-plans", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsPlanModel>>> getSavingsPlanList() {
        List<SavingsPlanModel> responseList = savingsPlanUseCases.savingsPlanList();
        ApiResponseJSON<List<SavingsPlanModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of saving plans.")
    @GetMapping(value = "/v1/common/savings-tenors", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsPlanTenorModel>>> getSavingsTenorList() {
        List<SavingsPlanTenorModel> responseList = savingsPlanUseCases.savingsTenorList();
        ApiResponseJSON<List<SavingsPlanTenorModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of saving goal categories.")
    @GetMapping(value = "/v1/common/savings-goal-categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsGoalCategoryModel>>> getSavingsGoalCategoryList() {
        List<SavingsGoalCategoryModel> responseList = savingsGoalCategoryUseCase.savingsGoalCategoryList();
        ApiResponseJSON<List<SavingsGoalCategoryModel>> apiResponseJSON = new ApiResponseJSON<>("Processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }
}
