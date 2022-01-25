package com.mintfintech.savingsms.usecase.features.spend_and_save.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SpendAndSaveSetUpRequest;
import com.mintfintech.savingsms.usecase.data.response.SpendAndSaveResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.spend_and_save.CreateSpendAndSaveUseCase;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Named
@AllArgsConstructor
public class CreateSpendAndSaveUseCaseImpl implements CreateSpendAndSaveUseCase {

    private final AppUserEntityDao appUserEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final SavingsGoalEntityDao savingsGoalEntityDao;
    private final SystemIssueLogService systemIssueLogService;
    private final SavingsPlanTenorEntityDao savingsPlanTenorEntityDao;
    private final SpendAndSaveEntityDao spendAndSaveEntityDao;
    private final SavingsGoalCategoryEntityDao savingsGoalCategoryEntityDao;
    private final SavingsPlanEntityDao savingsPlanEntityDao;


    @Override
    public SpendAndSaveResponse setUpSpendAndSave(AuthenticatedUser authenticatedUser, SpendAndSaveSetUpRequest setUpRequest) {
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        if (mintAccount.getAccountType() != AccountTypeConstant.INDIVIDUAL) {
            throw new BusinessLogicConflictException("Sorry, this feature is for individual account.");
        }

        int percentage = setUpRequest.getTransactionPercentage();
        int duration = setUpRequest.getDuration();
        if (percentage < 1 || duration < 30) {
            throw new BadRequestException("Minimum required percentage and duration is 1 and 30 respectively");
        }

        Optional<SpendAndSaveEntity> spendAndSaveSettingOptional = spendAndSaveEntityDao.findSpendAndSaveByAppUserAndMintAccount(appUser, mintAccount);
        SpendAndSaveEntity spendAndSaveEntity = new SpendAndSaveEntity();
        if (!spendAndSaveSettingOptional.isPresent()) {
            spendAndSaveEntity = SpendAndSaveEntity.builder()
                    .creator(appUser)
                    .account(mintAccount)
                    .dateActivated(LocalDateTime.now())
                    .percentage(percentage)
                    .activated(true)
                    .isSavingsLocked(setUpRequest.isSavingsLocked())
                    .build();
            spendAndSaveEntity = spendAndSaveEntityDao.saveRecord(spendAndSaveEntity);
        }

        Optional<SavingsGoalEntity> savingsGoalOptional = savingsGoalEntityDao.findFirstSavingsByType(mintAccount, SavingsGoalTypeConstant.SPEND_AND_SAVE);
        if (savingsGoalOptional.isPresent()) {
            String desc = "Account - "+mintAccount.getAccountId()+" is denied access to create spend and save. Spend and Save already exist";
            systemIssueLogService.logIssue("Critical - Recreating Spend and save settings", "Recreating Spend and save settings", desc);
            throw new BusinessLogicConflictException("Spend and save has already been setup.");
        }

        Optional<SavingsPlanTenorEntity> planTenorOpt = savingsPlanTenorEntityDao.findSavingsPlanTenorForDuration(duration);
        if(!planTenorOpt.isPresent()) {
            throw new BadRequestException("Select savings duration is not supported");
        }

        LocalDateTime maturityDate = LocalDateTime.now().plusDays(duration);

        SavingsGoalCategoryEntity goalCategoryEntity = savingsGoalCategoryEntityDao.findCategoryByCode("08")
                .orElseThrow(()-> new BusinessLogicConflictException("Savings goal category not found"));
        SavingsPlanEntity savingsPlanEntity = savingsPlanEntityDao.getPlanByType(SavingsPlanTypeConstant.SAVINGS_TIER_ONE);
        SavingsPlanTenorEntity planTenorEntity = planTenorOpt.get();
        SavingsGoalEntity savingsGoalEntity  = SavingsGoalEntity.builder()
                .savingsGoalType(SavingsGoalTypeConstant.SPEND_AND_SAVE)
                .savingsFrequency(SavingsFrequencyTypeConstant.NONE)
                .savingsPlan(savingsPlanEntity)
                .autoSave(false)
                .creationSource(SavingsGoalCreationSourceConstant.CUSTOMER)
                .goalStatus(SavingsGoalStatusConstant.ACTIVE)
                .targetAmount(BigDecimal.ZERO)
                .savingsBalance(BigDecimal.ZERO)
                .accruedInterest(BigDecimal.ZERO)
                .mintAccount(mintAccount)
                .name("Spend and save")
                .savingsPlanTenor(planTenorEntity)
                .interestRate(planTenorEntity.getInterestRate())
                .maturityDate(maturityDate)
                .selectedDuration(duration)
                .creator(appUser)
                .goalId(savingsGoalEntityDao.generateSavingGoalId())
                .savingsAmount(BigDecimal.ZERO)
                .goalCategory(goalCategoryEntity)
                .lockedSavings(setUpRequest.isSavingsLocked())
                .build();
        savingsGoalEntity = savingsGoalEntityDao.saveRecord(savingsGoalEntity);
        spendAndSaveEntity.setSavings(savingsGoalEntity);
        spendAndSaveEntityDao.saveRecord(spendAndSaveEntity);

        SpendAndSaveResponse response = SpendAndSaveResponse.builder()
                .amountSaved(savingsGoalEntity.getSavingsBalance())
                .status(savingsGoalEntity.getGoalStatus().name())
                .accruedInterest(savingsGoalEntity.getAccruedInterest())
                .build();

        if (savingsGoalEntity.getSavingsBalance().compareTo(BigDecimal.ZERO) <= 0) {
            response.setMaturityDate("");
        } else {
            response.setMaturityDate(savingsGoalEntity.getMaturityDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        return response;
    }
}
