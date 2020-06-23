package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsPlanTypeConstant;
import com.mintfintech.savingsms.domain.models.SavingsSearchDTO;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SavingsSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.AccountSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.PortalSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.models.MintSavingsGoalModel;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.utils.DateUtil;
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
import java.util.List;
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
    private SavingsPlanEntityDao savingsPlanEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;
    private AppUserEntityDao appUserEntityDao;
    private ApplicationProperty applicationProperty;

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

        boolean isMatured = isCustomerGoalMatured(savingsGoalEntity);

        SavingsGoalModel goalModel = new SavingsGoalModel();
        goalModel.setName(savingsGoalEntity.getName());
        goalModel.setGoalId(savingsGoalEntity.getGoalId());
        goalModel.setAccruedInterest(savingsGoalEntity.getAccruedInterest());
        goalModel.setTargetAmount(savingsGoalEntity.getTargetAmount());
        goalModel.setAvailableBalance(computeAvailableBalance(savingsGoalEntity, isMatured));
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

    private BigDecimal computeAvailableBalance(SavingsGoalEntity savingsGoalEntity, boolean matured) {
         if(matured) {
             return savingsGoalEntity.getSavingsBalance().add(savingsGoalEntity.getAccruedInterest());
         }
        /*long remainingDays = savingsGoalEntity.getDateCreated().until(LocalDateTime.now(), ChronoUnit.DAYS);
        int minimumDaysForWithdrawal = applicationProperty.savingsMinimumNumberOfDaysForWithdrawal();
        if(remainingDays >= minimumDaysForWithdrawal) {
            SavingsPlanEntity savingsPlanEntity = savingsGoalEntity.getSavingsPlan();
            return savingsGoalEntity.getSavingsBalance().subtract(savingsPlanEntity.getMinimumBalance());
        }*/
        return BigDecimal.valueOf(0.00);
    }

    private String getCustomerSavingsNoWithdrawalErrorMessage(SavingsGoalEntity savingsGoalEntity, boolean matured){
        if(matured){
            return  "";
        }
        return "Sorry, your savings goal is not yet due for withdrawal.";
    }

   private boolean isCustomerGoalMatured(SavingsGoalEntity savingsGoalEntity) {
       if(savingsGoalEntity.getMaturityDate() == null) {
           return false;
       }
       LocalDateTime maturityDate = savingsGoalEntity.getMaturityDate();
       if(DateUtil.sameDay(LocalDateTime.now(), maturityDate)) {
           return true;
       }
       return maturityDate.isBefore(LocalDateTime.now());
   }
   private boolean isMintGoalMatured(SavingsGoalEntity savingsGoalEntity) {
       boolean matured = false;
       if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS) {
           if(applicationProperty.isProductionEnvironment()) {
               matured = BigDecimal.valueOf(1000.00).compareTo(savingsGoalEntity.getSavingsBalance()) <= 0;
           }else {
               matured = BigDecimal.valueOf(20.00).compareTo(savingsGoalEntity.getSavingsBalance()) <= 0;
           }
       }
       return matured;
   }

   private String getMintGoalNoWithdrawalErrorMessage(SavingsGoalEntity savingsGoalEntity, boolean matured) {
        if(matured){
            return  "";
        }
        return "Sorry, the minimum amount for withdrawal is N1000.";
   }

    public MintSavingsGoalModel fromSavingsGoalEntityToMintGoalModel(SavingsGoalEntity savingsGoalEntity) {
        boolean matured = isMintGoalMatured(savingsGoalEntity);
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
                .filter(savingsGoalEntity -> savingsGoalEntity.getSavingsGoalType() != SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS)
                .map(this::fromSavingsGoalEntityToModel)
                .collect(Collectors.toList());
        return savingsGoalList;
    }

    @Override
    public List<SavingsGoalModel> getSavingsGoalList(AuthenticatedUser authenticatedUser) {
        MintAccountEntity mintAccountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        return getSavingsGoalList(mintAccountEntity);
    }

    @Override
    public AccountSavingsGoalResponse getAccountSavingsGoals(AuthenticatedUser authenticatedUser) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
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
            accountEntity = mintAccountEntityDao.findAccountByAccountId(searchRequest.getAccountId()).orElse(null);
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
}
