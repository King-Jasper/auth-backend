package com.mintfintech.savingsms.infrastructure.web.controllers;

import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.usecase.GetMintAccountUseCase;
import com.mintfintech.savingsms.usecase.data.response.MintBankAccountResponse;
import com.mintfintech.savingsms.usecase.master_record.InvestmentPlanUseCase;
import com.mintfintech.savingsms.usecase.master_record.SavingsGoalCategoryUseCase;
import com.mintfintech.savingsms.usecase.master_record.SavingsPlanUseCases;
import com.mintfintech.savingsms.usecase.models.InvestmentTenorModel;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

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
    private final InvestmentPlanUseCase investmentPlanUseCase;
    private final GetMintAccountUseCase getMintAccountUseCase;

    public MasterRecordsController(SavingsPlanUseCases savingsPlanUseCases,
                                   SavingsGoalCategoryUseCase savingsGoalCategoryUseCase,
                                   InvestmentPlanUseCase investmentPlanUseCase, GetMintAccountUseCase getMintAccountUseCase) {
        this.savingsPlanUseCases = savingsPlanUseCases;
        this.savingsGoalCategoryUseCase = savingsGoalCategoryUseCase;
        this.investmentPlanUseCase = investmentPlanUseCase;
        this.getMintAccountUseCase = getMintAccountUseCase;
    }

    @Deprecated
    @ApiOperation(value = "Returns a list of saving plans.")
    @GetMapping(value = "/v1/common/savings-plans", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsPlanDModel>>> getSavingsPlanDeprecatedList() {
        List<SavingsPlanDModel> responseList = savingsPlanUseCases.savingsPlanDeprecatedList();
        ApiResponseJSON<List<SavingsPlanDModel>> apiResponseJSON = new ApiResponseJSON<>("Saving plans processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of saving plans.")
    @GetMapping(value = "/v2/common/savings-plans", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsPlanModel>>> getSavingsPlanList() {
        List<SavingsPlanModel> responseList = savingsPlanUseCases.savingsPlanList();
        ApiResponseJSON<List<SavingsPlanModel>> apiResponseJSON = new ApiResponseJSON<>("Saving plans processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of saving plans.")
    @GetMapping(value = "/v1/common/savings-tenors", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsPlanTenorModel>>> getSavingsTenorList() {
        List<SavingsPlanTenorModel> responseList = savingsPlanUseCases.savingsTenorList();
        ApiResponseJSON<List<SavingsPlanTenorModel>> apiResponseJSON = new ApiResponseJSON<>("Saving tenors processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of saving goal categories.")
    @GetMapping(value = "/v1/common/savings-goal-categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<SavingsGoalCategoryModel>>> getSavingsGoalCategoryList() {
        List<SavingsGoalCategoryModel> responseList = savingsGoalCategoryUseCase.savingsGoalCategoryList();
        ApiResponseJSON<List<SavingsGoalCategoryModel>> apiResponseJSON = new ApiResponseJSON<>("Saving goal categories processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of investment tenors.")
    @GetMapping(value = "/v1/common/investment-tenors", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<InvestmentTenorModel>>> getInvestmentTenorList() {
        List<InvestmentTenorModel> responseList = investmentPlanUseCase.investmentTenorList();
        // removes flex investment.
        responseList = responseList.stream().filter(data -> !(data.getMinimumDuration() == 1 && data.getMaximumDuration() == 12)).collect(Collectors.toList());
        ApiResponseJSON<List<InvestmentTenorModel>> apiResponseJSON = new ApiResponseJSON<>("Investment tenors processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of investment tenors.")
    @GetMapping(value = "/v2/common/investment-tenors", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<List<InvestmentTenorModel>>> getInvestmentTenorListV2() {
        List<InvestmentTenorModel> responseList = investmentPlanUseCase.investmentTenorList();
        ApiResponseJSON<List<InvestmentTenorModel>> apiResponseJSON = new ApiResponseJSON<>("Investment tenors processed successfully.", responseList);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }

    @ApiOperation(value = "Returns a list of investment tenors.")
    @GetMapping(value = "/v1/common/mint-bank-account", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseJSON<MintBankAccountResponse>> getMintBankAccount(@RequestParam String accountNumber) {
        MintBankAccountResponse response = getMintAccountUseCase.getMintBankAccountResponse(accountNumber);
        ApiResponseJSON<MintBankAccountResponse> apiResponseJSON = new ApiResponseJSON<>("Retrieved successfully.", response);
        return new ResponseEntity<>(apiResponseJSON, HttpStatus.OK);
    }


}
