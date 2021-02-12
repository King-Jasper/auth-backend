package com.mintfintech.savingsms.usecase.features.referral_savings.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.CustomerReferralEvent;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import com.mintfintech.savingsms.usecase.features.referral_savings.CreateReferralRewardUseCase;
import com.mintfintech.savingsms.usecase.features.savings_funding.ReferralGoalFundingUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 15 Dec, 2020
 */
@Slf4j
@Named
@AllArgsConstructor
public class CreateReferralRewardUseCaseImpl implements CreateReferralRewardUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final CustomerReferralEntityDao customerReferralEntityDao;
    private final SavingsGoalEntityDao savingsGoalEntityDao;
    private final SavingsPlanEntityDao planEntityDao;
    private final SavingsGoalCategoryEntityDao categoryEntityDao;
    private final SavingsPlanTenorEntityDao planTenorEntityDao;
    private final ApplicationProperty applicationProperty;
    private final ReferralGoalFundingUseCase referralGoalFundingUseCase;
    private final SettingsEntityDao settingsEntityDao;
    private final SystemIssueLogService systemIssueLogService;

    private static final String SIDE_HUSTLE_REFERRAL_CODE = "SIDEHUSTLE";
    private static final String VALENTINE_REFERRAL_CODE = "JOMOJUWA"; //"VALGIVEAWAY";


    public void processReferralByUser(String userId, int size, boolean overrideTime) {
        Optional<AppUserEntity> appUserEntityOpt = appUserEntityDao.findAppUserByUserId(userId);
        if(!appUserEntityOpt.isPresent()) {
            log.info("User Id not found.");
            return;
        }
        AppUserEntity appUserEntity = appUserEntityOpt.get();
        MintAccountEntity referral = appUserEntity.getPrimaryAccount();

        Optional<SavingsGoalEntity> goalEntityOpt = savingsGoalEntityDao.findFirstSavingsByTypeIgnoreStatus(referral, SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS);
        SavingsGoalEntity referralSavingsGoalEntity = goalEntityOpt.orElseGet(() -> createSavingsGoal(referral, appUserEntity));
        LocalDateTime start, end;
        if(overrideTime) {
            start = LocalDateTime.of(LocalDate.of(2021, 2, 8), LocalTime.of(21, 0));
            end = LocalDateTime.now();
        }else {
            start = LocalDateTime.of(LocalDate.of(2021, 2, 8), LocalTime.of(21, 30));
            end = LocalDateTime.of(LocalDate.of(2021, 2, 9), LocalTime.of(9, 30));
        }

        List<CustomerReferralEntity> referralList = customerReferralEntityDao.getByReferral(referral, start, end, size);
        log.info("LIST PULLED - {}, start - {}, end - {}", referralList.size(), start, end);
        if(!referralList.isEmpty()) {
            if(referralSavingsGoalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
                referralSavingsGoalEntity.setRecordStatus(RecordStatusConstant.ACTIVE);
                referralSavingsGoalEntity.setGoalStatus(SavingsGoalStatusConstant.ACTIVE);
                savingsGoalEntityDao.saveRecord(referralSavingsGoalEntity);
            }
        }
        for(CustomerReferralEntity record : referralList) {
           if(record.isReferrerRewarded()) {
               continue;
           }
            /*
            BigDecimal total = referralSavingsGoalEntity.getTotalAmountWithdrawn() == null ? BigDecimal.ZERO: referralSavingsGoalEntity.getTotalAmountWithdrawn();
            total = total.add(referralSavingsGoalEntity.getSavingsBalance());
            if(total.doubleValue() >= 10000.00) {
               continue;
            }*/
            long referralRewardAmount = applicationProperty.getReferralRewardAmount();
            SavingsGoalFundingResponse fundingResponse = referralGoalFundingUseCase.fundReferralSavingsGoal(referralSavingsGoalEntity, BigDecimal.valueOf(referralRewardAmount));
            log.info("credit response code - {}", fundingResponse.getResponseCode());
            if("00".equalsIgnoreCase(fundingResponse.getResponseCode())) {
                record.setReferrerRewarded(true);
                customerReferralEntityDao.saveRecord(record);
            }
           // referralSavingsGoalEntity = savingsGoalEntityDao.getRecordById(referralSavingsGoalEntity.getId());
        }
    }

    @Async
    @Override
    public void processCustomerReferralReward(CustomerReferralEvent referralEvent) {
        boolean referralRewardEnabled = Boolean.parseBoolean(settingsEntityDao.getSettings(SettingsNameTypeConstant.REFERRAL_REWARD_ENABLED, "true"));
        if(!referralRewardEnabled) {
            log.info("Referral reward not enabled.");
        }
        try {
            Thread.sleep(2500);
            // this allows for the new customer details published to be created completely.
        }catch (Exception ignored){}
        if(StringUtils.isEmpty(referralEvent.getReferredByUserId())) {
            log.info("Referral userId not found - {}", referralEvent.toString());
            return;
        }
        AppUserEntity userEntity = appUserEntityDao.getAppUserByUserId(referralEvent.getReferredByUserId());
        MintAccountEntity referralAccount = mintAccountEntityDao.getRecordById(userEntity.getPrimaryAccount().getId());
        boolean canCreateRecord = shouldProceed(referralAccount, referralEvent.getReferralCodeUsed());
        if(!canCreateRecord){
            return;
        }
        //long accountReferred = customerReferralEntityDao.totalReferralRecordsForAccount(referralAccount);
        Optional<MintAccountEntity> referredOpt = mintAccountEntityDao.findAccountByAccountId(referralEvent.getAccountId());
        if(!referredOpt.isPresent()) {
            log.info("referred detail not found - {}", referralEvent.toString());
            return;
        }
        MintAccountEntity referredAccount = referredOpt.get();
        if(customerReferralEntityDao.recordExistForAccounts(referralAccount, referredAccount)) {
            log.info("referral record is already created. {}", referralEvent.toString());
            return;
        }
        CustomerReferralEntity referralEntity = CustomerReferralEntity.builder()
                .referralCode(referralEvent.getReferralCodeUsed())
                .referred(referredAccount)
                .referrer(referralAccount)
                .referredRewarded(false)
                .referrerRewarded(false)
                .referralCode(referralEvent.getReferralCodeUsed())
                .build();
        referralEntity = customerReferralEntityDao.saveRecord(referralEntity);

        /*if(accountReferred >= 10) {
           // BigDecimal total = referralSavingsGoalEntity.getTotalAmountWithdrawn() == null ? BigDecimal.ZERO: referralSavingsGoalEntity.getTotalAmountWithdrawn();
           // total = total.add(referralSavingsGoalEntity.getSavingsBalance());
            String message = "AccountId - "+referralAccount.getAccountId()+" Account Name - "+referralAccount.getName()+" code - "+referralEvent.getReferredByUserId()+" " +
                    "count - "+accountReferred;
            systemIssueLogService.logIssue("Suspicious Referral", "Suspicious Referral", message);
            // referralEntity.setReferrerRewarded(true);
            // customerReferralEntityDao.saveRecord(referralEntity);
        }*/
        /*
        Optional<SavingsGoalEntity> goalEntityOpt = savingsGoalEntityDao.findFirstSavingsByTypeIgnoreStatus(referralAccount, SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS);
        SavingsGoalEntity referralSavingsGoalEntity = goalEntityOpt.orElseGet(() -> createSavingsGoal(referralAccount, userEntity));
        if(accountReferred >= 7) {
            BigDecimal total = referralSavingsGoalEntity.getTotalAmountWithdrawn() == null ? BigDecimal.ZERO: referralSavingsGoalEntity.getTotalAmountWithdrawn();
            total = total.add(referralSavingsGoalEntity.getSavingsBalance());
            String message = "AccountId - "+referralAccount.getAccountId()+" Account Name - "+referralAccount.getName()+" code - "+referralEvent.getReferredByUserId()+" " +
                    "count - "+accountReferred+" amount gotten - "+total.toPlainString();
            systemIssueLogService.logIssue("Suspicious Referral", "Suspicious Referral", message);

            if(total.doubleValue() >= 10000.0) {
                log.info("referral aborted");
                 return;
            }
        }
        if(referralSavingsGoalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
            referralSavingsGoalEntity.setRecordStatus(RecordStatusConstant.ACTIVE);
            referralSavingsGoalEntity.setGoalStatus(SavingsGoalStatusConstant.ACTIVE);
            savingsGoalEntityDao.saveRecord(referralSavingsGoalEntity);
        }


        long referralRewardAmount = applicationProperty.getReferralRewardAmount();
        SavingsGoalFundingResponse fundingResponse = referralGoalFundingUseCase.fundReferralSavingsGoal(referralSavingsGoalEntity, BigDecimal.valueOf(referralRewardAmount));
        if("00".equalsIgnoreCase(fundingResponse.getResponseCode())) {
            referralEntity.setReferrerRewarded(true);
            customerReferralEntityDao.saveRecord(referralEntity);
        }
        */
        /*
        Optional<AppUserEntity> referredUserOpt = appUserEntityDao.findAccountOwner(referredAccount);
        if(!referredUserOpt.isPresent()) {
            return;
        }
        AppUserEntity referredUser = referredUserOpt.get();
        goalEntityOpt = savingsGoalEntityDao.findFirstSavingsByType(referredAccount, SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS);
        SavingsGoalEntity referredSavingsGoalEntity = goalEntityOpt.orElseGet(() -> createSavingsGoal(referredAccount, referredUser));
        long referredRewardAmount = applicationProperty.getReferredRewardAmount();

        fundingResponse = fundSavingsGoalUseCase.fundReferralSavingsGoal(referredSavingsGoalEntity, BigDecimal.valueOf(referredRewardAmount));
        if("00".equalsIgnoreCase(fundingResponse.getResponseCode())) {
            referralEntity.setReferredRewarded(true);
            customerReferralEntityDao.saveRecord(referralEntity);
        }*/
    }

    @Async
    @Override
    public void processReferredCustomerReward(MintAccountEntity referredAccount, SavingsGoalEntity fundedSavingsGoal) {

        if(fundedSavingsGoal.getSavingsGoalType() != SavingsGoalTypeConstant.CUSTOMER_SAVINGS) {
            log.info("Savings is not customer savings goal");
            return;
        }
        BigDecimal minimumFunding = BigDecimal.valueOf(1000.00);
        BigDecimal goalBalance = fundedSavingsGoal.getSavingsBalance();
        if(goalBalance.compareTo(minimumFunding) < 0) {
            log.info("Savings balance {} is lower than minimum balance {}", goalBalance, minimumFunding);
            return;
        }

        Optional<CustomerReferralEntity> referralEntityOpt = customerReferralEntityDao.findUnprocessedReferredAccountReward(referredAccount);
        if(!referralEntityOpt.isPresent()) {
            return;
        }
        CustomerReferralEntity referralEntity = referralEntityOpt.get();
        /*
        Optional<SavingsGoalEntity> goalEntityOpt = savingsGoalEntityDao.findFirstSavingsByTypeIgnoreStatus(referredAccount, SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS);
        Optional<AppUserEntity> referredUserOpt = appUserEntityDao.findAccountOwner(referredAccount);
        if(!referredUserOpt.isPresent()) {
            return;
        }
        AppUserEntity referredUser = referredUserOpt.get();
        SavingsGoalEntity referredSavingsGoalEntity = goalEntityOpt.orElseGet(() -> createSavingsGoal(referredAccount, referredUser));
        if(referredSavingsGoalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
            referredSavingsGoalEntity.setRecordStatus(RecordStatusConstant.ACTIVE);
            referredSavingsGoalEntity.setGoalStatus(SavingsGoalStatusConstant.ACTIVE);
            savingsGoalEntityDao.saveRecord(referredSavingsGoalEntity);
        }
        long referredRewardAmount = applicationProperty.getReferredRewardAmount();
        SavingsGoalFundingResponse fundingResponse = referralGoalFundingUseCase.fundReferralSavingsGoal(referredSavingsGoalEntity, BigDecimal.valueOf(referredRewardAmount));
        if("00".equalsIgnoreCase(fundingResponse.getResponseCode())) {
            referralEntity.setReferredRewarded(true);
            customerReferralEntityDao.saveRecord(referralEntity);
        }
        */

        if(referralEntity.isReferrerRewarded()) {
            return;
        }
        MintAccountEntity referralAccount = mintAccountEntityDao.getRecordById(referralEntity.getReferrer().getId());
        Optional<AppUserEntity> referralUserOpt = appUserEntityDao.findAccountOwner(referralAccount);
        if(!referralUserOpt.isPresent()) {
            return;
        }
        AppUserEntity referralUser = referralUserOpt.get();
        Optional<SavingsGoalEntity> goalEntityOpt = savingsGoalEntityDao.findFirstSavingsByTypeIgnoreStatus(referralAccount, SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS);
        SavingsGoalEntity referralSavingsGoalEntity = goalEntityOpt.orElseGet(() -> createSavingsGoal(referralAccount, referralUser));
        if(referralSavingsGoalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
            referralSavingsGoalEntity.setRecordStatus(RecordStatusConstant.ACTIVE);
            referralSavingsGoalEntity.setGoalStatus(SavingsGoalStatusConstant.ACTIVE);
            savingsGoalEntityDao.saveRecord(referralSavingsGoalEntity);
        }

        long referralRewardAmount = applicationProperty.getReferralRewardAmount();
        SavingsGoalFundingResponse fundingResponse = referralGoalFundingUseCase.fundReferralSavingsGoal(referralSavingsGoalEntity, BigDecimal.valueOf(referralRewardAmount));
        if("00".equalsIgnoreCase(fundingResponse.getResponseCode())) {
            referralEntity.setReferrerRewarded(true);
            customerReferralEntityDao.saveRecord(referralEntity);
        }

    }

    private SavingsGoalEntity createSavingsGoal(MintAccountEntity accountEntity, AppUserEntity userEntity) {
        int minimumDuration = 30;
        SavingsPlanTenorEntity planTenor = planTenorEntityDao.findSavingsPlanTenorForDuration(minimumDuration).get();
        SavingsGoalCategoryEntity goalCategoryEntity = categoryEntityDao.findCategoryByCode("08").get();
        SavingsPlanEntity savingsPlanEntity = planEntityDao.getPlanByType(SavingsPlanTypeConstant.SAVINGS_TIER_ONE);
        SavingsGoalEntity savingsGoalEntity  = SavingsGoalEntity.builder()
                .savingsGoalType(SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS)
                .savingsFrequency(SavingsFrequencyTypeConstant.NONE)
                .savingsPlan(savingsPlanEntity)
                .autoSave(false)
                .creationSource(SavingsGoalCreationSourceConstant.MINT)
                .goalStatus(SavingsGoalStatusConstant.ACTIVE)
                .targetAmount(BigDecimal.ZERO)
                .savingsBalance(BigDecimal.ZERO)
                .accruedInterest(BigDecimal.ZERO)
                .mintAccount(accountEntity)
                .name("Earnings from Referral")
                .savingsPlanTenor(planTenor)
                .selectedDuration(minimumDuration)
                .creator(userEntity)
                .goalId(savingsGoalEntityDao.generateSavingGoalId())
                .savingsAmount(BigDecimal.ZERO)
                .goalCategory(goalCategoryEntity)
                .totalAmountWithdrawn(BigDecimal.ZERO)
                .lockedSavings(false)
                .build();
        return savingsGoalEntityDao.saveRecord(savingsGoalEntity);
    }

    private boolean shouldProceed(MintAccountEntity referral, String referralCode) {
        if(SIDE_HUSTLE_REFERRAL_CODE.equalsIgnoreCase(referralCode)) {
            log.info("Side hustle referral code used -- abort");
            return false;
        }
        if(VALENTINE_REFERRAL_CODE.equalsIgnoreCase(referralCode)) {
            log.info("Valentine give away referral code used -- abort");
            return false;
        }
        Optional<CustomerReferralEntity> valReferralRecordOpt = customerReferralEntityDao.findRecordByReferralCodeAndReferredAccount(VALENTINE_REFERRAL_CODE, referral);
        return !valReferralRecordOpt.isPresent();
    }
}
