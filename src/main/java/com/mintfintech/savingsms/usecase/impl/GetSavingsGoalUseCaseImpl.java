package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.SavingsSearchDTO;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.ComputeAvailableAmountUseCase;
import com.mintfintech.savingsms.usecase.GetMintAccountUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.MintAccountRecordRequestEvent;
import com.mintfintech.savingsms.usecase.data.request.SavingsSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.SavingsWithdrawalRequest;
import com.mintfintech.savingsms.usecase.data.response.AccountSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.PortalSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalWithdrawalResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.features.roundup_savings.GetRoundUpSavingsUseCase;
import com.mintfintech.savingsms.usecase.models.EmergencySavingModel;
import com.mintfintech.savingsms.usecase.models.MintSavingsGoalModel;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.models.SavingsTransactionModel;
import com.mintfintech.savingsms.utils.PhoneNumberUtils;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;

import javax.inject.Named;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by jnwanya on
 * Wed, 01 Apr, 2020
 */
@FieldDefaults(makeFinal = true)
@AllArgsConstructor
@Named
public class GetSavingsGoalUseCaseImpl implements GetSavingsGoalUseCase {

    private SavingsPlanTenorEntityDao savingsPlanTenorEntityDao;
    private SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private SavingsWithdrawalRequestEntityDao savingsWithdrawalRequestEntityDao;
    private SavingsInterestEntityDao savingsInterestEntityDao;
    private SavingsPlanEntityDao savingsPlanEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;
    private AppUserEntityDao appUserEntityDao;
    private ApplicationProperty applicationProperty;
    private ComputeAvailableAmountUseCase computeAvailableAmountUseCase;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private ApplicationEventService applicationEventService;
    private final GetMintAccountUseCase getMintAccountUseCase;

    @Override
    public SavingsGoalModel fromSavingsGoalEntityToModel(SavingsGoalEntity savingsGoalEntity) {
        SavingsPlanEntity savingsPlanEntity = savingsPlanEntityDao.getRecordById(savingsGoalEntity.getSavingsPlan().getId());
        SavingsPlanTenorEntity planTenorEntity = savingsPlanTenorEntityDao.getRecordById(savingsGoalEntity.getSavingsPlanTenor().getId());
        String maturityDate = "";
        String nextSavingsDate = "";
        if(savingsGoalEntity.getMaturityDate() != null) {
            maturityDate = savingsGoalEntity.getMaturityDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if(savingsGoalEntity.getNextAutoSaveDate() != null) {
            nextSavingsDate = savingsGoalEntity.getNextAutoSaveDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        BigDecimal interest = savingsGoalEntity.getAccruedInterest().setScale(2, RoundingMode.UP);
        BigDecimal withHoldingTax = BigDecimal.ZERO;
        LocalDateTime whtChargeStartDate = LocalDateTime.of(2023, 10, 21, 0, 0, 0);
        if(savingsGoalEntity.getDateCreated().isAfter(whtChargeStartDate) && savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.CUSTOMER_SAVINGS) {
            withHoldingTax = interest.multiply(BigDecimal.valueOf(0.1));
        }
        boolean isMatured = computeAvailableAmountUseCase.isMaturedSavingsGoal(savingsGoalEntity);
        SavingsGoalModel goalModel = new SavingsGoalModel();
        goalModel.setName(savingsGoalEntity.getName());
        goalModel.setGoalId(savingsGoalEntity.getGoalId());
        goalModel.setAccruedInterest(interest);
        goalModel.setWithholdingTax(withHoldingTax);
        goalModel.setTargetAmount(savingsGoalEntity.getTargetAmount());
        goalModel.setAvailableBalance(computeAvailableAmountUseCase.getAvailableAmount(savingsGoalEntity));
        goalModel.setAutoSaveEnabled(savingsGoalEntity.isAutoSave());
        goalModel.setInterestRate(planTenorEntity.getInterestRate());
        goalModel.setCategoryCode(savingsGoalEntity.getGoalCategory().getCode());
        goalModel.setCurrentStatus(savingsGoalEntity.getGoalStatus().name());
        goalModel.setStartDate(savingsGoalEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE));
        goalModel.setNextSavingsDate(nextSavingsDate);
        goalModel.setMaturityDate(maturityDate);
        goalModel.setSavingsAmount(savingsGoalEntity.getSavingsAmount());
        goalModel.setSavingsBalance(savingsGoalEntity.getSavingsBalance());
        goalModel.setSavingPlanName(savingsPlanEntity.getPlanName().getName());
        goalModel.setSavingsType(savingsGoalEntity.getSavingsGoalType().name().replace("_", " "));
        goalModel.setSavingFrequency(savingsGoalEntity.getSavingsFrequency() != null ? savingsGoalEntity.getSavingsFrequency().name() : "");
        goalModel.setNoWithdrawalErrorMessage(getCustomerSavingsNoWithdrawalErrorMessage(savingsGoalEntity, isMatured));
        goalModel.setLockedSavings(savingsGoalEntity.isLockedSavings());
        // goalModel.setChosenSavingsDurationInDays(savingsGoalEntity.getSelectedDuration());
        if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.EMERGENCY_SAVINGS) {
            goalModel.setInterestRate(0.0);
            if(savingsGoalEntity.getGoalStatus() == SavingsGoalStatusConstant.ACTIVE && savingsGoalEntity.getSavingsBalance().compareTo(BigDecimal.ZERO) > 0) {
               goalModel.setCurrentStatus(SavingsGoalStatusConstant.MATURED.name());
            }
        }
        return goalModel;
    }

    @Override
    public PortalSavingsGoalResponse fromSavingsGoalEntityToPortalSavingsGoalResponse (SavingsGoalEntity savingsGoalEntity) {
        PortalSavingsGoalResponse goalResponse = fromSavingsGoalEntityToModel(savingsGoalEntity);
        MintAccountEntity mintAccountEntity = mintAccountEntityDao.getRecordById(savingsGoalEntity.getMintAccount().getId());
        AppUserEntity appUserEntity = appUserEntityDao.getRecordById(savingsGoalEntity.getCreator().getId());
        goalResponse.setAccountId(mintAccountEntity.getAccountId());
        goalResponse.setUserId(appUserEntity.getUserId());
        goalResponse.setCustomerName(appUserEntity.getName());
        return goalResponse;
    }


    private String getCustomerSavingsNoWithdrawalErrorMessage(SavingsGoalEntity savingsGoalEntity, boolean matured){
        if(matured){
            return  "";
        }
        if(savingsGoalEntity.isLockedSavings()) {
            return "Sorry, your savings goal is not yet matured for withdrawal.";
        }
        long savingsDuration = savingsGoalEntity.getDateCreated().until(LocalDateTime.now(), ChronoUnit.DAYS);
        if(savingsDuration >=  applicationProperty.savingsMinimumNumberOfDaysForWithdrawal()) {
            return "";
        }
        long remainingDays = applicationProperty.savingsMinimumNumberOfDaysForWithdrawal() - savingsDuration;
        return "Sorry, your savings goal will be available for withdrawal in "+remainingDays+" day(s) time";
    }


   private String getMintGoalNoWithdrawalErrorMessage(SavingsGoalEntity savingsGoalEntity, boolean matured) {
        if(matured){
            return  "";
        }
        if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.ROUND_UP_SAVINGS) {
            long remainingDays = LocalDateTime.now().until(savingsGoalEntity.getMaturityDate(), ChronoUnit.DAYS);
            return "Sorry, your savings will be matured in "+remainingDays+" day(s) time";
        }else {
            if(applicationProperty.isProductionEnvironment() || applicationProperty.isStagingEnvironment()) {
                return "Sorry, the minimum amount for withdrawal is N1000.";
            }else {
                return "Sorry, the minimum amount for withdrawal is N1000.";
            }
        }

   }

    public MintSavingsGoalModel fromSavingsGoalEntityToMintGoalModel(SavingsGoalEntity savingsGoalEntity) {
        boolean matured = computeAvailableAmountUseCase.isMaturedSavingsGoal(savingsGoalEntity);
        BigDecimal accruedInterest = savingsGoalEntity.getAccruedInterest();
        if(accruedInterest.compareTo(BigDecimal.ZERO) > 0) {
            accruedInterest = accruedInterest.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        }
        BigDecimal savingsBalance = savingsGoalEntity.getSavingsBalance();
        if(savingsBalance.compareTo(BigDecimal.ZERO) > 0) {
            savingsBalance = savingsBalance.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        }
        String maturityDate = "";
        if(savingsGoalEntity.getMaturityDate() != null) {
            maturityDate = savingsGoalEntity.getMaturityDate().format(DateTimeFormatter.ISO_DATE_TIME);
        }
        BigDecimal availableBalance = matured ? savingsGoalEntity.getSavingsBalance().add(savingsGoalEntity.getAccruedInterest()) : BigDecimal.valueOf(0.00);
        return MintSavingsGoalModel.builder()
                .goalId(savingsGoalEntity.getGoalId())
                .name(savingsGoalEntity.getName())
                .savingsBalance(savingsBalance)
                .accruedInterest(accruedInterest)
                .availableBalance(availableBalance)
                .maturityDate(maturityDate)
                .noWithdrawalErrorMessage(getMintGoalNoWithdrawalErrorMessage(savingsGoalEntity, matured))
                .matured(matured).build();
    }

    @Override
    public SavingsGoalModel getSavingsGoalByGoalId(AuthenticatedUser authenticatedUser, String goalId) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        SavingsGoalEntity savingsGoal = savingsGoalEntityDao.findSavingGoalByAccountAndGoalId(accountEntity, goalId)
                .orElseThrow(() -> new BadRequestException("Invalid savings goal Id."));
        return fromSavingsGoalEntityToModel(savingsGoal);
    }

    @Override
    public List<SavingsGoalModel> getSavingsGoalList(MintAccountEntity mintAccountEntity) {
        List<SavingsGoalModel> savingsGoalList = savingsGoalEntityDao.getAccountSavingGoals(mintAccountEntity)
                .stream()
                .filter(savingsGoalEntity -> (savingsGoalEntity.getCreationSource() != SavingsGoalCreationSourceConstant.MINT && savingsGoalEntity.getSavingsGoalType() != SavingsGoalTypeConstant.EMERGENCY_SAVINGS))
                .map(this::fromSavingsGoalEntityToModel)
                .collect(Collectors.toList());
        return savingsGoalList;
    }

    @Override
    public List<SavingsGoalModel> getSavingsGoalList(AuthenticatedUser authenticatedUser) {
        MintAccountEntity accountEntity = getMintAccountUseCase.getMintAccount(authenticatedUser);
        return getSavingsGoalList(accountEntity);
    }


    @Override
    public AccountSavingsGoalResponse getAccountSavingsGoals(AuthenticatedUser authenticatedUser) {
        MintAccountEntity accountEntity = getMintAccountUseCase.getMintAccount(authenticatedUser);
        List<MintSavingsGoalModel> mintGoalsList = new ArrayList<>();
        List<SavingsGoalModel> savingsGoalList = new ArrayList<>();
        List<SavingsGoalEntity> savingsGoalEntityList = savingsGoalEntityDao.getAccountSavingGoals(accountEntity);

        EmergencySavingModel emergencySaving = EmergencySavingModel.builder().exist(false).build();
        for(SavingsGoalEntity savingsGoalEntity : savingsGoalEntityList) {
            if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.EMERGENCY_SAVINGS) {
                emergencySaving.setExist(true);
                emergencySaving.setSavingsGoal(fromSavingsGoalEntityToModel(savingsGoalEntity));
            }else if(savingsGoalEntity.getCreationSource() == SavingsGoalCreationSourceConstant.CUSTOMER) {
                 savingsGoalList.add(fromSavingsGoalEntityToModel(savingsGoalEntity));
            }else if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS){
                mintGoalsList.add(fromSavingsGoalEntityToMintGoalModel(savingsGoalEntity));
                /*if(savingsGoalEntity.getSavingsGoalType() != SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS){
                    mintGoalsList.add(fromSavingsGoalEntityToMintGoalModel(savingsGoalEntity));
                }*/
            }else if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.ROUND_UP_SAVINGS) {
                mintGoalsList.add(fromSavingsGoalEntityToMintGoalModel(savingsGoalEntity));
            }
        }
        return AccountSavingsGoalResponse.builder()
                .customerGoals(savingsGoalList)
                .mintGoals(mintGoalsList)
                .emergencySaving(emergencySaving)
                .build();
    }


    @Override
    public PortalSavingsGoalResponse getPortalSavingsGoalResponseByGoalId(String goalId) {
        SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.findSavingGoalByGoalId(goalId).orElseThrow(() -> new NotFoundException("Savings goal with provided goalId does not exist."));
        return fromSavingsGoalEntityToPortalSavingsGoalResponse(savingsGoalEntity);
    }

    @Override
    public PagedDataResponse<PortalSavingsGoalResponse> getPagedSavingsGoals(SavingsSearchRequest searchRequest, int page, int size) {
        MintAccountEntity accountEntity = null;
        if(!StringUtils.isEmpty(searchRequest.getAccountId())) {
            accountEntity = mintAccountEntityDao.findAccountByAccountId(searchRequest.getAccountId()).orElseThrow(()->new BadRequestException("Invalid account Id"));
        }
        if(StringUtils.isNotEmpty(searchRequest.getAccountNumber())) {
            MintBankAccountEntity bankAccountEntity = mintBankAccountEntityDao.findByAccountNumber(searchRequest.getAccountNumber())
                    .orElseThrow(() -> new NotFoundException("Account number does not exist."));
            accountEntity = bankAccountEntity.getMintAccount();
        }
        SavingsGoalTypeConstant goalType = null;
        if(!StringUtils.isEmpty(searchRequest.getSavingsType()) && !searchRequest.getSavingsType().equalsIgnoreCase("ALL")) {
            goalType = SavingsGoalTypeConstant.valueOf(searchRequest.getSavingsType());
        }
        SavingsSearchDTO.AutoSaveStatus autoSaveStatus = null;
        if(!"ALL".equalsIgnoreCase(searchRequest.getAutoSavedStatus())) {
            autoSaveStatus = SavingsSearchDTO.AutoSaveStatus.valueOf(searchRequest.getAutoSavedStatus());
        }
        String phoneNumber = searchRequest.getPhoneNumber();
        if(StringUtils.isNotEmpty(phoneNumber)) {
            phoneNumber = PhoneNumberUtils.toInternationalFormat(phoneNumber);
        }
        SavingsSearchDTO searchDTO = SavingsSearchDTO.builder()
                .goalId(searchRequest.getGoalId())
                .account(accountEntity)
                .autoSaveStatus(autoSaveStatus)
                .customerName(searchRequest.getCustomerName())
                .phoneNumber(phoneNumber)
                .goalName(searchRequest.getGoalName())
                .goalStatus(SavingsGoalStatusConstant.valueOf(searchRequest.getSavingsStatus()))
                .goalType(goalType)
                .fromDate(searchRequest.getFromDate() != null ? searchRequest.getFromDate().atStartOfDay() : null)
                .toDate(searchRequest.getToDate() != null ? searchRequest.getToDate().atTime(23, 59): null)
                .build();

        BigDecimal amountSaved = savingsGoalEntityDao.sumSearchedSavingsGoal(searchDTO);
        Page<SavingsGoalEntity> goalEntityPage = savingsGoalEntityDao.searchSavingsGoal(searchDTO, page, size);

        return new PagedDataResponse<>(
                goalEntityPage.getTotalElements(),
                goalEntityPage.getTotalPages(),
                amountSaved,
                goalEntityPage.get().map(this::fromSavingsGoalEntityToPortalSavingsGoalResponse)
                        .collect(Collectors.toList()));
    }


    @Override
    public PagedDataResponse<SavingsTransactionModel> getSavingsTransactions(String goalId, int page, int size) {
        SavingsGoalEntity goalEntity = savingsGoalEntityDao.findSavingGoalByGoalId(goalId)
                .orElseThrow(() -> new NotFoundException("Invalid savings goal Id."));
        Page<SavingsGoalTransactionEntity> transactionEntityPage = savingsGoalTransactionEntityDao.getTransactions(goalEntity, page, size);
        return new PagedDataResponse<>(transactionEntityPage.getTotalElements(), transactionEntityPage.getTotalPages(),
                transactionEntityPage.get().map(this::fromSavingTransactionToModel)
        .collect(Collectors.toList()));
    }

    @Override
    public PagedDataResponse<SavingsInterestModel> getSavingsInterest(String goalId, int page, int size) {
        SavingsGoalEntity goalEntity = savingsGoalEntityDao.findSavingGoalByGoalId(goalId)
                .orElseThrow(() -> new NotFoundException("Invalid savings goal Id."));
        Page<SavingsInterestEntity> interestPage = savingsInterestEntityDao.getAccruedInterestOnGoal(goalEntity, page, size);
        return new PagedDataResponse<>(interestPage.getTotalElements(), interestPage.getTotalPages(),
                interestPage.get().map(interestEntity -> SavingsInterestModel.builder()
                        .interestAmount(interestEntity.getInterest())
                        .rate(interestEntity.getRate())
                        .savingsBalance(interestEntity.getSavingsBalance())
                        .interestDate(interestEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                        .build())
                .collect(Collectors.toList()));
    }

    private SavingsTransactionModel fromSavingTransactionToModel(SavingsGoalTransactionEntity transactionEntity) {
        FundingSourceTypeConstant fundingSource = FundingSourceTypeConstant.MINT_ACCOUNT;
        if(transactionEntity.getFundingSource() != null) {
            fundingSource = transactionEntity.getFundingSource();
        }
        String fundingSourceName = fundingSource == FundingSourceTypeConstant.MINT_ACCOUNT ? "MINTYN ACCOUNT" : fundingSource.name();
        return SavingsTransactionModel.builder()
                .amount(transactionEntity.getTransactionAmount())
                .transactionDate(transactionEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .reference(transactionEntity.getTransactionReference())
                .savingsBalance(transactionEntity.getNewBalance())
                .transactionStatus(transactionEntity.getTransactionStatus().name())
                .transactionType(transactionEntity.getTransactionType().name())
                .fundingSource(fundingSourceName)
                .build();
    }
}
