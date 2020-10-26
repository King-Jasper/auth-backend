package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsPlanTypeConstant;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.SavingsSearchDTO;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.ComputeAvailableAmountUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.MintAccountRecordRequestEvent;
import com.mintfintech.savingsms.usecase.data.request.SavingsSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.AccountSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.PortalSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.models.MintSavingsGoalModel;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.models.SavingsTransactionModel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;

import javax.inject.Named;
import java.math.BigDecimal;
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
    private SavingsInterestEntityDao savingsInterestEntityDao;
    private SavingsPlanEntityDao savingsPlanEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;
    private AppUserEntityDao appUserEntityDao;
    private ApplicationProperty applicationProperty;
    private ComputeAvailableAmountUseCase computeAvailableAmountUseCase;
    private ApplicationEventService applicationEventService;

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

        boolean isMatured = computeAvailableAmountUseCase.isMaturedSavingsGoal(savingsGoalEntity);
        SavingsGoalModel goalModel = new SavingsGoalModel();
        goalModel.setName(savingsGoalEntity.getName());
        goalModel.setGoalId(savingsGoalEntity.getGoalId());
        goalModel.setAccruedInterest(savingsGoalEntity.getAccruedInterest());
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
        goalModel.setSavingFrequency(savingsGoalEntity.getSavingsFrequency() != null ? savingsGoalEntity.getSavingsFrequency().name() : "");
        goalModel.setNoWithdrawalErrorMessage(getCustomerSavingsNoWithdrawalErrorMessage(savingsGoalEntity, isMatured));
        goalModel.setLockedSavings(savingsGoalEntity.isLockedSavings());
        // goalModel.setChosenSavingsDurationInDays(savingsGoalEntity.getSelectedDuration());
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
            return "Sorry, your savings goal is not yet due matured for withdrawal.";
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
            if(applicationProperty.isProductionEnvironment() || applicationProperty.isStagingEnvironment()) {
                return "Sorry, the minimum amount for withdrawal is N1000.";
            }else {
                return "Sorry, the minimum amount for withdrawal is N100.";
            }
        }else {
            if(applicationProperty.isProductionEnvironment() || applicationProperty.isStagingEnvironment()) {
                return "Sorry, the minimum amount for withdrawal is N1000.";
            }else {
                return "Sorry, the minimum amount for withdrawal is N20.";
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
        BigDecimal availableBalance = matured ? savingsGoalEntity.getSavingsBalance().add(savingsGoalEntity.getAccruedInterest()) : BigDecimal.valueOf(0.00);
        return MintSavingsGoalModel.builder()
                .goalId(savingsGoalEntity.getGoalId())
                .name(savingsGoalEntity.getName())
                .savingsBalance(savingsBalance)
                .accruedInterest(accruedInterest)
                .availableBalance(availableBalance)
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
                .filter(savingsGoalEntity -> savingsGoalEntity.getCreationSource() != SavingsGoalCreationSourceConstant.MINT)
                .map(this::fromSavingsGoalEntityToModel)
                .collect(Collectors.toList());
        return savingsGoalList;
    }

    @Override
    public List<SavingsGoalModel> getSavingsGoalList(AuthenticatedUser authenticatedUser) {
        MintAccountEntity accountEntity = getMintAccount(authenticatedUser);
        return getSavingsGoalList(accountEntity);
    }

    private MintAccountEntity getMintAccount(AuthenticatedUser authenticatedUser) {
        Optional<MintAccountEntity> accountEntityOptional = mintAccountEntityDao.findAccountByAccountId(authenticatedUser.getAccountId());
        if(!accountEntityOptional.isPresent()) {
            MintAccountRecordRequestEvent requestEvent = MintAccountRecordRequestEvent.builder()
                    .topicNameSuffix("savings-service")
                    .accountIds(Collections.singletonList(authenticatedUser.getAccountId()))
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.MISSING_ACCOUNT_RECORD, new EventModel<>(requestEvent));
            throw new UnauthorisedException("Invalid accountId.");
        }
        return accountEntityOptional.get();
    }

    @Override
    public AccountSavingsGoalResponse getAccountSavingsGoals(AuthenticatedUser authenticatedUser) {
        MintAccountEntity accountEntity = getMintAccount(authenticatedUser);
        List<MintSavingsGoalModel> mintGoalsList = new ArrayList<>();
        List<SavingsGoalModel> savingsGoalList = new ArrayList<>();
        List<SavingsGoalEntity> savingsGoalEntityList = savingsGoalEntityDao.getAccountSavingGoals(accountEntity);
        for(SavingsGoalEntity savingsGoalEntity : savingsGoalEntityList) {
            if(savingsGoalEntity.getCreationSource() == SavingsGoalCreationSourceConstant.CUSTOMER) {
                 savingsGoalList.add(fromSavingsGoalEntityToModel(savingsGoalEntity));
            }else {
               mintGoalsList.add(fromSavingsGoalEntityToMintGoalModel(savingsGoalEntity));
            }
        }
        return AccountSavingsGoalResponse.builder()
                .customerGoals(savingsGoalList)
                .mintGoals(mintGoalsList)
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
        SavingsPlanEntity savingsPlan = null;
        if(!StringUtils.isEmpty(searchRequest.getSavingsTier()) && !searchRequest.getSavingsTier().equalsIgnoreCase("ALL")) {
            SavingsPlanTypeConstant planType = SavingsPlanTypeConstant.valueOf(searchRequest.getSavingsTier());
            savingsPlan = savingsPlanEntityDao.getPlanByType(planType);
        }
        SavingsSearchDTO.AutoSaveStatus autoSaveStatus = null;
        if(!"ALL".equalsIgnoreCase(searchRequest.getAutoSavedStatus())) {
            autoSaveStatus = SavingsSearchDTO.AutoSaveStatus.valueOf(searchRequest.getAutoSavedStatus());
        }
        SavingsSearchDTO searchDTO = SavingsSearchDTO.builder()
                .goalId(searchRequest.getGoalId())
                .account(accountEntity)
                .autoSaveStatus(autoSaveStatus)
                .goalStatus(SavingsGoalStatusConstant.valueOf(searchRequest.getSavingsStatus()))
                .savingsPlan(savingsPlan)
                .fromDate(searchRequest.getFromDate() != null ? searchRequest.getFromDate().atStartOfDay() : null)
                .toDate(searchRequest.getToDate() != null ? searchRequest.getToDate().atTime(23, 59): null)
                .build();

        Page<SavingsGoalEntity> goalEntityPage = savingsGoalEntityDao.searchSavingsGoal(searchDTO, page, size);
        return new PagedDataResponse<>(goalEntityPage.getTotalElements(), goalEntityPage.getTotalPages(),
                goalEntityPage.get().map(this::fromSavingsGoalEntityToPortalSavingsGoalResponse)
                        .collect(Collectors.toList()));
    }


    @Override
    public PagedDataResponse<SavingsTransactionModel> getSavingsTransactions(String goalId, int page, int size) {
        SavingsGoalEntity goalEntity = savingsGoalEntityDao.findSavingGoalByGoalId(goalId)
                .orElseThrow(() -> new NotFoundException("Invalid savings goal Id."));
        Page<SavingsGoalTransactionEntity> transactionEntityPage = savingsGoalTransactionEntityDao.getTransactions(goalEntity, page, size);
        return new PagedDataResponse<>(transactionEntityPage.getTotalElements(), transactionEntityPage.getTotalElements(),
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
                        .build())
                .collect(Collectors.toList()));
    }

    private SavingsTransactionModel fromSavingTransactionToModel(SavingsGoalTransactionEntity transactionEntity) {
        return SavingsTransactionModel.builder()
                .amount(transactionEntity.getTransactionAmount())
                .automated(transactionEntity.getPerformedBy() == null)
                .reference(transactionEntity.getTransactionReference())
                .savingsBalance(transactionEntity.getNewBalance())
                .transactionStatus(transactionEntity.getTransactionStatus().name())
                .transactionType(transactionEntity.getTransactionType().name())
                .build();
    }
}
