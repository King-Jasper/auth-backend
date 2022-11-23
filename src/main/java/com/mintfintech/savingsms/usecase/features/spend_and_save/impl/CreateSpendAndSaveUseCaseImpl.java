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

        double percentage = setUpRequest.getTransactionPercentage();
        int duration = setUpRequest.getDuration();
        boolean isSavingsLocked = setUpRequest.isSavingsLocked();
        if (percentage <= 0.0) {
            throw new BadRequestException("Percentage must be greater than 0.");
        }
        if (isSavingsLocked) {
            if (duration < 30) {
                throw new BadRequestException("Minimum required duration is 30 days");
            }
        }

        SpendAndSaveEntity spendAndSave = null;
        SavingsGoalEntity savingsGoal = null;
        Optional<SpendAndSaveEntity> spendAndSaveSettingOptional = spendAndSaveEntityDao.findSpendAndSaveByAppUserAndMintAccount(appUser, mintAccount);
        if (spendAndSaveSettingOptional.isPresent()) {
            spendAndSave = spendAndSaveSettingOptional.get();
            if(spendAndSave.getRecordStatus() == RecordStatusConstant.ACTIVE) {
                throw new BusinessLogicConflictException("You already have a spend and save record");
            }
            savingsGoal = spendAndSave.getSavings();
        }

        if(savingsGoal == null) {
            Optional<SavingsGoalEntity> savingsGoalOptional = savingsGoalEntityDao.findFirstSavingsByType(mintAccount, SavingsGoalTypeConstant.SPEND_AND_SAVE);
            if (savingsGoalOptional.isPresent()) {
                savingsGoal = savingsGoalOptional.get();
              //  String desc = "Account - "+mintAccount.getAccountId()+" is denied access to create spend and save. Spend and Save already exist";
              //  systemIssueLogService.logIssue("Critical - Recreating Spend and save settings", "Recreating Spend and save settings", desc);
              //  throw new BusinessLogicConflictException("Spend and save has already been setup.");
            }
        }

        LocalDateTime maturityDate = null;
        SavingsPlanTenorEntity planTenorEntity = savingsPlanTenorEntityDao.findSavingsPlanTenorForDuration(30).get();
        double interestRate = 0.0;
        if (isSavingsLocked) {
            Optional<SavingsPlanTenorEntity> planTenorOpt = savingsPlanTenorEntityDao.findSavingsPlanTenorForDuration(duration);
            if (!planTenorOpt.isPresent()) {
                throw new BadRequestException("Select savings duration is not supported");
            }
            planTenorEntity = planTenorOpt.get();
            interestRate = planTenorEntity.getInterestRate();
            maturityDate = LocalDateTime.now().plusDays(duration);
        }

        SavingsGoalCategoryEntity goalCategoryEntity = savingsGoalCategoryEntityDao.findCategoryByCode("08")
                .orElseThrow(()-> new BusinessLogicConflictException("Savings goal category not found"));
        SavingsPlanEntity savingsPlanEntity = savingsPlanEntityDao.getPlanByType(SavingsPlanTypeConstant.SAVINGS_TIER_ONE);

        if(savingsGoal == null) {
            savingsGoal = new SavingsGoalEntity();
            savingsGoal.setGoalId(savingsGoalEntityDao.generateSavingGoalId());
            savingsGoal.setSavingsAmount(BigDecimal.ZERO);
            savingsGoal.setTotalAmountWithdrawn(BigDecimal.ZERO);
        }
        savingsGoal.setSavingsGoalType(SavingsGoalTypeConstant.SPEND_AND_SAVE);
        savingsGoal.setSavingsFrequency(SavingsFrequencyTypeConstant.NONE);
        savingsGoal.setLockedSavings(isSavingsLocked);
        savingsGoal.setRecordStatus(RecordStatusConstant.ACTIVE);
        savingsGoal.setGoalStatus(SavingsGoalStatusConstant.ACTIVE);
        savingsGoal.setSavingsPlan(savingsPlanEntity);
        savingsGoal.setAutoSave(false);
        savingsGoal.setCreationSource(SavingsGoalCreationSourceConstant.MINT);
        savingsGoal.setTargetAmount(BigDecimal.ZERO);
        savingsGoal.setSavingsBalance(BigDecimal.ZERO);
        savingsGoal.setAccruedInterest(BigDecimal.ZERO);
        savingsGoal.setMintAccount(mintAccount);
        savingsGoal.setName("Spend and save");
        savingsGoal.setSavingsPlanTenor(planTenorEntity);
        savingsGoal.setInterestRate(interestRate);
        savingsGoal.setMaturityDate(maturityDate);
        savingsGoal.setSelectedDuration(duration);
        savingsGoal.setCreator(appUser);
        savingsGoal.setGoalCategory(goalCategoryEntity);
        savingsGoal.setDateCreated(LocalDateTime.now());
        savingsGoal = savingsGoalEntityDao.saveRecord(savingsGoal);

        /*
        SavingsGoalEntity savingsGoalEntity  = SavingsGoalEntity.builder()
                .savingsGoalType(SavingsGoalTypeConstant.SPEND_AND_SAVE)
                .savingsFrequency(SavingsFrequencyTypeConstant.NONE)
                .savingsPlan(savingsPlanEntity)
                .autoSave(false)
                .creationSource(SavingsGoalCreationSourceConstant.MINT)
                .goalStatus(SavingsGoalStatusConstant.ACTIVE)
                .targetAmount(BigDecimal.ZERO)
                .savingsBalance(BigDecimal.ZERO)
                .accruedInterest(BigDecimal.ZERO)
                .mintAccount(mintAccount)
                .name("Spend and save")
                .savingsPlanTenor(planTenorEntity)
                .interestRate(interestRate)
                .maturityDate(maturityDate)
                .selectedDuration(duration)
                .creator(appUser)
                .goalId(savingsGoalEntityDao.generateSavingGoalId())
                .savingsAmount(BigDecimal.ZERO)
                .totalAmountWithdrawn(BigDecimal.ZERO)
                .goalCategory(goalCategoryEntity)
                .lockedSavings(isSavingsLocked)
                .build();
        savingsGoalEntity = savingsGoalEntityDao.saveRecord(savingsGoalEntity);
        */
        if(spendAndSave == null) {
            spendAndSave = new SpendAndSaveEntity();
        }
        spendAndSave.setCreator(appUser);
        spendAndSave.setAccount(mintAccount);
        spendAndSave.setDateActivated(LocalDateTime.now());
        spendAndSave.setPercentage(percentage);
        spendAndSave.setActivated(true);
        spendAndSave.setSavings(savingsGoal);


        /*
        SpendAndSaveEntity spendAndSaveEntity = SpendAndSaveEntity.builder()
                .creator(appUser)
                .account(mintAccount)
                .dateActivated(LocalDateTime.now())
                .percentage(percentage)
                .activated(true)
                .isSavingsLocked(isSavingsLocked)
                .savings(savingsGoalEntity)
                .build();
        spendAndSaveEntity = spendAndSaveEntityDao.saveRecord(spendAndSaveEntity);
        */


        BigDecimal amountSaved = savingsGoal.getSavingsBalance();
        BigDecimal accruedInterest = savingsGoal.getAccruedInterest();

        SpendAndSaveResponse response = SpendAndSaveResponse.builder()
                .exist(true)
                .amountSaved(amountSaved)
                .status(savingsGoal.getGoalStatus().name())
                .accruedInterest(accruedInterest)
                .isSavingsLocked(spendAndSave.isSavingsLocked())
                .totalAmount(amountSaved.add(accruedInterest))
                .percentage(spendAndSave.getPercentage())
                .build();

        if (savingsGoal.getSavingsBalance().compareTo(BigDecimal.ZERO) <= 0 || savingsGoal.getMaturityDate() == null) {
            response.setMaturityDate("");
        } else {
            response.setMaturityDate(savingsGoal.getMaturityDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        return response;
    }
}
