package com.mintfintech.savingsms.usecase.features.referral_savings.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.reports.ReferralRewardStat;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.CustomerReferralEvent;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import com.mintfintech.savingsms.usecase.features.referral_savings.CreateReferralRewardUseCase;
import com.mintfintech.savingsms.usecase.features.savings_funding.ReferralGoalFundingUseCase;
import com.mintfintech.savingsms.utils.PhoneNumberUtils;
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
import java.util.Random;

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
    private final SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final ReactHQReferralEntityDao reactHQReferralEntityDao;

    private static final String SIDE_HUSTLE_REFERRAL_CODE = "SIDEHUSTLE";
    private static final String MINT_ANNIVERSARY_REFERRAL_CODE = "MINT365";
    private static final String CERA_PLUG_REFERRAL_CODE = "OUKONU";
    private static final String VALENTINE_REFERRAL_CODE = "JOMOJUWA"; //"VALGIVEAWAY";
    private static final String REACTHQ_REFERRAL_CODE = "REACTHQ";

    private static final BigDecimal referralAmount = BigDecimal.valueOf(500.00);
    private static final BigDecimal minimumFundAmount = BigDecimal.valueOf(250.00);


    @Async
    public void processReferralBackLog(LocalDateTime start, LocalDateTime end, int size) {

        List<CustomerReferralEntity> referralList = customerReferralEntityDao.getUnprocessedRecordByReferral(start, end, size, minimumFundAmount);
        for(CustomerReferralEntity record: referralList) {

            Optional<SavingsGoalEntity> goalEntityOpt = savingsGoalEntityDao.findFirstSavingsByTypeIgnoreStatus(record.getReferrer(), SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS);
            AppUserEntity appUserEntity = appUserEntityDao.findAccountOwner(record.getReferrer()).orElse(null);
            MintBankAccountEntity bankAccountEntity = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(record.getReferrer(), BankAccountTypeConstant.CURRENT);
            if(bankAccountEntity.getAccountTierLevel().getLevel() == TierLevelTypeConstant.TIER_ONE) {
                continue;
            }
            MintBankAccountEntity referredAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(record.getReferred(), BankAccountTypeConstant.CURRENT);
            if(referredAccount.getAccountTierLevel().getLevel() == TierLevelTypeConstant.TIER_ONE) {
                continue;
            }
            SavingsGoalEntity referralSavingsGoalEntity = goalEntityOpt.orElseGet(() -> createSavingsGoal(record.getReferrer(), appUserEntity));
            
            boolean processed = processReferralPayment(record, referralSavingsGoalEntity);
            if(processed && referralSavingsGoalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
                referralSavingsGoalEntity.setRecordStatus(RecordStatusConstant.ACTIVE);
                referralSavingsGoalEntity.setGoalStatus(SavingsGoalStatusConstant.ACTIVE);
                savingsGoalEntityDao.saveRecord(referralSavingsGoalEntity);
            }
        }
    }

    public String processReferralByUser(String userId, String phoneNumber,  int size, boolean overrideTime) {

        LocalDateTime start = LocalDate.of(2021, 3, 14).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        Optional<AppUserEntity> appUserEntityOpt;
        if(StringUtils.isNotEmpty(phoneNumber)) {
            if(!phoneNumber.startsWith("+")) {
                phoneNumber = PhoneNumberUtils.toInternationalFormat(phoneNumber);
            }
            log.info("Phone Number - {}",phoneNumber);
            appUserEntityOpt = appUserEntityDao.findUserByPhoneNumber(phoneNumber);
        }else {
           appUserEntityOpt = appUserEntityDao.findAppUserByUserId(userId);
        }
        if(!appUserEntityOpt.isPresent()) {
            log.info("User Id not found.");
            return "User not found";
        }
        AppUserEntity appUserEntity = appUserEntityOpt.get();
        MintAccountEntity referral = appUserEntity.getPrimaryAccount();

        Optional<SavingsGoalEntity> goalEntityOpt = savingsGoalEntityDao.findFirstSavingsByTypeIgnoreStatus(referral, SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS);
        SavingsGoalEntity referralSavingsGoalEntity = goalEntityOpt.orElseGet(() -> createSavingsGoal(referral, appUserEntity));

        List<ReferralRewardStat> rewardStats = customerReferralEntityDao.getReferralRewardStatOnAccount(referral);
        long totalProcessed = 0, totalUnProcessed = 0;
        for(ReferralRewardStat rewardStat: rewardStats) {
            if(rewardStat.isProcessed()) {
                totalProcessed = rewardStat.getCount();
            }else {
                totalUnProcessed = rewardStat.getCount();
            }
        }
        long totalRecords = totalProcessed + totalUnProcessed;
        String beforeProcessing = String.format("BEFORE UPDATE - Total Referrals - %d, Total Processed - %d", totalRecords, totalProcessed);

        List<CustomerReferralEntity> referralList = customerReferralEntityDao.getUnprocessedRecordByReferral(referral, start, end, size);
        log.info("LIST PULLED - {}, start - {}, end - {}", referralList.size(), start, end);
        for(CustomerReferralEntity record : referralList) {
           boolean processed =  processReferralPayment(record, referralSavingsGoalEntity);
           if(processed && referralSavingsGoalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
               referralSavingsGoalEntity.setRecordStatus(RecordStatusConstant.ACTIVE);
               referralSavingsGoalEntity.setGoalStatus(SavingsGoalStatusConstant.ACTIVE);
               savingsGoalEntityDao.saveRecord(referralSavingsGoalEntity);
           }
        }

        rewardStats = customerReferralEntityDao.getReferralRewardStatOnAccount(referral);
        totalProcessed = 0; totalUnProcessed = 0;
        for(ReferralRewardStat rewardStat: rewardStats) {
            if(rewardStat.isProcessed()) {
                totalProcessed = rewardStat.getCount();
            }else {
                totalUnProcessed = rewardStat.getCount();
            }
        }
        totalRecords = totalProcessed + totalUnProcessed;

        String afterProcessing = String.format("Total Referrals - %d, Total Processed - %d", totalRecords, totalProcessed);

        return String.format("BEFORE UPDATE : %s | AFTER UPDATE : %s", beforeProcessing, afterProcessing);
    }

    private boolean processReferralPayment(CustomerReferralEntity record, SavingsGoalEntity referralSavingsGoalEntity) {

        if(record.isReferrerRewarded()) {
            log.info("referrer rewarded for record - {}", record.getId());
            return false;
        }
        if(StringUtils.defaultString(record.getRegistrationPlatform()).equalsIgnoreCase("WEB")) {
            log.info("is web referral for record - {}", record.getId());
            return false;
        }

        Optional<SavingsGoalEntity> tempOpt = savingsGoalEntityDao.findFirstSavingsByType(record.getReferred(), SavingsGoalTypeConstant.CUSTOMER_SAVINGS);
        if(!tempOpt.isPresent()) {
            log.info("no savings goal found for account - {}", record.getReferred().getId());
            return false;
        }
        SavingsGoalEntity temp = tempOpt.get();

        LocalDateTime newProgramDate = LocalDate.of(2021, 9, 17).atTime(0, 0);
        boolean newProgram = true;
        Optional<SavingsGoalTransactionEntity> transactionOpt = savingsGoalTransactionEntityDao.findFirstTransactionForSavings(temp);
        if(transactionOpt.isPresent()) {
            SavingsGoalTransactionEntity transactionEntity = transactionOpt.get();
            if(transactionEntity.getDateCreated().isBefore(newProgramDate)) {
                newProgram = false;
            }
        }

        BigDecimal minAmount = minimumFundAmount;
        BigDecimal payoutAmount = referralAmount;

        if(!newProgram) {
            minAmount = BigDecimal.valueOf(500.00);
            payoutAmount = BigDecimal.valueOf(1000.00);
        }

        BigDecimal goalBalance = temp.getSavingsBalance();
        if(goalBalance.compareTo(minAmount) < 0) {
            log.info("Savings {} balance {} is lower than minimum balance {}", temp.getGoalId(), goalBalance, minAmount);
            return false;
        }
        SavingsGoalFundingResponse fundingResponse = referralGoalFundingUseCase.fundReferralSavingsGoal(referralSavingsGoalEntity, payoutAmount);
        log.info("credit response code - {}", fundingResponse.getResponseCode());
        if("00".equalsIgnoreCase(fundingResponse.getResponseCode())) {
            record.setReferrerRewarded(true);
            customerReferralEntityDao.saveRecord(record);
            return true;
        }
        return false;
    }

    //@Async
    @Override
    public void processCustomerReferralReward(CustomerReferralEvent referralEvent) {

        try {
            int seconds = new Random().nextInt(500) + 2000;
            Thread.sleep(seconds);
            // this allows for the new customer details published to be created completely.
        }catch (Exception ignored){}

        String referralCode = referralEvent.getReferralCodeUsed();
        if(REACTHQ_REFERRAL_CODE.equalsIgnoreCase(referralCode)) {
            reactHQReferralProcessing(referralEvent);
            return;
        }

        boolean referralRewardEnabled = Boolean.parseBoolean(settingsEntityDao.getSettings(SettingsNameTypeConstant.REFERRAL_REWARD_ENABLED, "true"));
        if(!referralRewardEnabled) {
            log.info("Referral reward not enabled.");
        }
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
        if(customerReferralEntityDao.recordExistForReferredAccount(referredAccount)) {
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
                .registrationPlatform(referralEvent.getRegistrationPlatform())
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
        /// stopped automatic payout.

        LocalDateTime newProgramDate = LocalDate.of(2021, 3, 24).atStartOfDay();
        if(fundedSavingsGoal.getSavingsGoalType() != SavingsGoalTypeConstant.CUSTOMER_SAVINGS) {
            log.info("Savings is not customer savings goal");
            return;
        }

        BigDecimal goalBalance = fundedSavingsGoal.getSavingsBalance();
        if(goalBalance.compareTo(minimumFundAmount) < 0) {
            log.info("Savings balance {} is lower than minimum balance {}", goalBalance, minimumFundAmount);
            return;
        }

        Optional<CustomerReferralEntity> referralEntityOpt = customerReferralEntityDao.findUnprocessedReferredAccountReward(referredAccount);
        if(!referralEntityOpt.isPresent()) {
            return;
        }
        CustomerReferralEntity referralEntity = referralEntityOpt.get();

        if(referralEntity.isReferrerRewarded()) {
            return;
        }
        if(StringUtils.defaultString(referralEntity.getRegistrationPlatform()).equalsIgnoreCase("WEB")) {
            return;
        }
        MintAccountEntity referralAccount = mintAccountEntityDao.getRecordById(referralEntity.getReferrer().getId());
        MintBankAccountEntity referralBankAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(referralAccount, BankAccountTypeConstant.CURRENT);
        if(referralBankAccount.getAccountTierLevel().getLevel() == TierLevelTypeConstant.TIER_ONE) {
            return;
        }
        MintBankAccountEntity referredBankAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(referredAccount, BankAccountTypeConstant.CURRENT);
        if(referredBankAccount.getAccountTierLevel().getLevel() == TierLevelTypeConstant.TIER_ONE) {
            return;
        }
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
        SavingsGoalFundingResponse fundingResponse = referralGoalFundingUseCase.fundReferralSavingsGoal(referralSavingsGoalEntity, referralAmount);
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
                .interestRate(planTenor.getInterestRate())
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
        if(SIDE_HUSTLE_REFERRAL_CODE.equalsIgnoreCase(referralCode) ||
                CERA_PLUG_REFERRAL_CODE.equalsIgnoreCase(referralCode) ||
                MINT_ANNIVERSARY_REFERRAL_CODE.equalsIgnoreCase(referralCode) ||
                REACTHQ_REFERRAL_CODE.equalsIgnoreCase(referralCode)) {
            log.info("Side hustle referral code used {} -- abort ", referralCode);
            return false;
        }
        /*if(VALENTINE_REFERRAL_CODE.equalsIgnoreCase(referralCode)) {
            log.info("Valentine give away referral code used -- abort");
            return false;
        }
        Optional<CustomerReferralEntity> valReferralRecordOpt = customerReferralEntityDao.findRecordByReferralCodeAndReferredAccount(VALENTINE_REFERRAL_CODE, referral);
        return !valReferralRecordOpt.isPresent();
        */
        return true;
    }

    private void reactHQReferralProcessing(CustomerReferralEvent referralEvent) {
        Optional<MintAccountEntity> mintAccountOpt  = mintAccountEntityDao.findAccountByAccountId(referralEvent.getAccountId());
        if(!mintAccountOpt.isPresent()) {
            return;
        }
        MintAccountEntity mintAccount = mintAccountOpt.get();
        if(reactHQReferralEntityDao.findRecordByCustomer(mintAccount).isPresent()) {
            return;
        }
        String platform = referralEvent.getRegistrationPlatform();
        ReactHQReferralEntity referralEntity = ReactHQReferralEntity.builder()
                .customer(mintAccount)
                .customerCredited(false)
                .registrationPlatform(platform)
                .customerDebited(false)
                .build();
        reactHQReferralEntityDao.saveRecord(referralEntity);
    }
}
