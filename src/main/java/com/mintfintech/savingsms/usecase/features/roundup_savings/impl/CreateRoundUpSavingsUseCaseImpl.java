package com.mintfintech.savingsms.usecase.features.roundup_savings.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.request.RoundUpSavingSetUpRequest;
import com.mintfintech.savingsms.usecase.data.response.RoundUpSavingResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.roundup_savings.CreateRoundUpSavingsUseCase;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
/**
 * Created by jnwanya on
 * Sat, 31 Oct, 2020
 */
@Slf4j
@Named
@AllArgsConstructor
public class CreateRoundUpSavingsUseCaseImpl implements CreateRoundUpSavingsUseCase {

    private final GetSavingsGoalUseCase getSavingsGoalUseCase;
    private final RoundUpSavingsSettingEntityDao roundUpSavingsSettingEntityDao;
    private final SystemIssueLogService systemIssueLogService;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final SavingsGoalEntityDao savingsGoalEntityDao;
    private final SavingsPlanEntityDao savingsPlanEntityDao;
    private final SavingsPlanTenorEntityDao savingsPlanTenorEntityDao;
    private final SavingsGoalCategoryEntityDao savingsGoalCategoryEntityDao;


    @Override
    public RoundUpSavingResponse setupRoundUpSavings(AuthenticatedUser authenticatedUser, RoundUpSavingSetUpRequest request) {

        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        if(accountEntity.getAccountType() != AccountTypeConstant.INDIVIDUAL) {
            throw new BusinessLogicConflictException("Sorry, this feature is for individual account.");
        }
        AppUserEntity appUserEntity = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        RoundUpSavingsTypeConstant roundUpSavingsType = RoundUpSavingsTypeConstant.valueOf(request.getRoundUpType());

        int selectedDuration = request.getDuration();
        if(selectedDuration < 90) {
            throw new BadRequestException("Minimum of 90 days required for RoundUp savings setup");
        }

        Optional<SavingsGoalEntity> roundUpSavingsOpt = savingsGoalEntityDao.findFirstSavingsByType(accountEntity, SavingsGoalTypeConstant.ROUND_UP_SAVINGS);
        if(roundUpSavingsOpt.isPresent()) {
            log.info("This shouldn't ever happen.");
            String desc = "Account - "+accountEntity.getAccountId()+" is denied access to create roundup savings. Roundup saving already exist";
            systemIssueLogService.logIssue("Critical - Recreating Roundup settings", "Recreating Roundup settings", desc);
            throw new BusinessLogicConflictException("Round up savings has already been setup.");
        }

        Optional<SavingsPlanTenorEntity> planTenorOpt = savingsPlanTenorEntityDao.findSavingsPlanTenorForDuration(selectedDuration);
        if(!planTenorOpt.isPresent()) {
            throw new BadRequestException("Select savings duration is not supported");
        }

        Optional<RoundUpSavingsSettingEntity> settingEntityOpt = roundUpSavingsSettingEntityDao.findRoundUpSavingsByUser(appUserEntity);
        RoundUpSavingsSettingEntity settingEntity;
        if(!settingEntityOpt.isPresent()) {
            settingEntity = new RoundUpSavingsSettingEntity();
            settingEntity.setAccount(accountEntity);
            settingEntity.setCreator(appUserEntity);
        }else {
            settingEntity = settingEntityOpt.get();
        }
        settingEntity.setFundTransferRoundUpType(roundUpSavingsType);
        settingEntity.setBillPaymentRoundUpType(roundUpSavingsType);
        settingEntity.setCardPaymentRoundUpType(roundUpSavingsType);
        settingEntity.setEnabled(true);
        settingEntity.setDateActivated(LocalDateTime.now());
        settingEntity = roundUpSavingsSettingEntityDao.saveRecord(settingEntity);

        LocalDateTime maturityDate = LocalDateTime.now().plusDays(selectedDuration);

        SavingsGoalCategoryEntity goalCategoryEntity = savingsGoalCategoryEntityDao.findCategoryByCode("08").get();
        SavingsPlanEntity savingsPlanEntity = savingsPlanEntityDao.getPlanByType(SavingsPlanTypeConstant.SAVINGS_TIER_ONE);
        SavingsPlanTenorEntity planTenorEntity = planTenorOpt.get();
        SavingsGoalEntity savingsGoalEntity  = SavingsGoalEntity.builder()
                .savingsGoalType(SavingsGoalTypeConstant.ROUND_UP_SAVINGS)
                .savingsFrequency(SavingsFrequencyTypeConstant.NONE)
                .savingsPlan(savingsPlanEntity)
                .autoSave(false)
                .creationSource(SavingsGoalCreationSourceConstant.MINT)
                .goalStatus(SavingsGoalStatusConstant.ACTIVE)
                .targetAmount(BigDecimal.ZERO)
                .savingsBalance(BigDecimal.ZERO)
                .accruedInterest(BigDecimal.ZERO)
                .mintAccount(accountEntity)
                .name("Round-Up Savings")
                .savingsPlanTenor(planTenorEntity)
                .maturityDate(maturityDate)
                .selectedDuration(selectedDuration)
                .creator(appUserEntity)
                .goalId(savingsGoalEntityDao.generateSavingGoalId())
                .savingsAmount(BigDecimal.ZERO)
                .goalCategory(goalCategoryEntity)
                .lockedSavings(true)
                .build();
        savingsGoalEntity = savingsGoalEntityDao.saveRecord(savingsGoalEntity);

        settingEntity.setRoundUpSavings(savingsGoalEntity);
        roundUpSavingsSettingEntityDao.saveRecord(settingEntity);

        SavingsGoalModel savingsGoalModel = getSavingsGoalUseCase.fromSavingsGoalEntityToModel(savingsGoalEntity);

        return RoundUpSavingResponse.builder()
                .exist(true)
                .id(settingEntity.getId())
                .isActive(true)
                .roundUpType(roundUpSavingsType.getName())
                .savingsGoal(savingsGoalModel)
                .build();
    }
}
