package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.PagedResponse;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.InterestAccruedUpdateRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.usecase.ApplySavingsInterestUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalBalanceUpdateEvent;
import com.mintfintech.savingsms.utils.DateUtil;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@FieldDefaults(makeFinal = true)
@Slf4j
@Named
@AllArgsConstructor
public class ApplySavingsInterestUseCaseImpl implements ApplySavingsInterestUseCase {

    private SavingsGoalEntityDao savingsGoalEntityDao;
    private ApplicationEventService applicationEventService;
    private SavingsInterestEntityDao savingsInterestEntityDao;
    private SavingsPlanTenorEntityDao savingsPlanTenorEntityDao;
    private AccumulatedInterestEntityDao accumulatedInterestEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;
    private CoreBankingServiceClient coreBankingServiceClient;
    private SystemIssueLogService systemIssueLogService;

    @Override
    public void processInterestAndUpdateGoals() {
        int size = 50;
        BigDecimal totalAccumulatedInterest = BigDecimal.valueOf(0.00);
        PagedResponse<SavingsGoalEntity> pagedResponse = savingsGoalEntityDao.getPagedEligibleInterestSavingsGoal(0, size);
        totalAccumulatedInterest = totalAccumulatedInterest.add(processInterestComputation(pagedResponse.getRecords()));
        int totalPages = pagedResponse.getTotalPages();
        if(pagedResponse.getTotalRecords() > 0) {
            log.info("Savings goal for interest consideration: {}", pagedResponse.getTotalRecords());
        }
        for(int i = 1; i < totalPages; i++) {
            pagedResponse = savingsGoalEntityDao.getPagedEligibleInterestSavingsGoal(i, size);
            totalAccumulatedInterest = totalAccumulatedInterest.add(processInterestComputation(pagedResponse.getRecords()));
        }
        updateInterestLiabilityAccountWithAccumulatedInterest(totalAccumulatedInterest);
    }
    private BigDecimal processInterestComputation(List<SavingsGoalEntity> savingsGoalEntityList) {
        BigDecimal totalInterest = BigDecimal.valueOf(0.0);
        for(SavingsGoalEntity savingsGoalEntity: savingsGoalEntityList) {
             if(!shouldApplyInterest(savingsGoalEntity)) {
                 log.info("Interest not applied to goal {}: {}", savingsGoalEntity.getGoalId(), savingsGoalEntity.getName());
                 continue;
             }
             BigDecimal interest = applyInterest(savingsGoalEntity);
             totalInterest = totalInterest.add(interest);
             publishInterestApplication(savingsGoalEntity);
        }
        return totalInterest;
    }

    private BigDecimal applyInterest(SavingsGoalEntity savingsGoalEntity) {
        SavingsPlanTenorEntity planTenorEntity = savingsPlanTenorEntityDao.getRecordById(savingsGoalEntity.getSavingsPlanTenor().getId());
        BigDecimal interestRatePerDay = BigDecimal.valueOf(planTenorEntity.getInterestRate() / (100.0 * 365.0));
        BigDecimal interest = savingsGoalEntity.getSavingsBalance().multiply(interestRatePerDay).setScale(2, BigDecimal.ROUND_HALF_EVEN);

        SavingsInterestEntity savingsInterestEntity = SavingsInterestEntity.builder()
                .interest(interest)
                .savingsBalance(savingsGoalEntity.getSavingsBalance())
                .savingsGoal(savingsGoalEntity)
                .rate(planTenorEntity.getInterestRate())
                .build();
        savingsInterestEntityDao.saveRecord(savingsInterestEntity);
        savingsGoalEntity.setAccruedInterest(savingsInterestEntityDao.getTotalInterestAmountOnGoal(savingsGoalEntity));
        savingsGoalEntity.setLastInterestApplicationDate(LocalDateTime.now());
        savingsGoalEntityDao.saveRecord(savingsGoalEntity);
        return interest;
    }

    private boolean shouldApplyInterest(SavingsGoalEntity savingsGoalEntity) {
       /* if(savingsGoalEntity.getCreationSource() != SavingsGoalCreationSourceConstant.CUSTOMER) {
            log.info("Saving goal not created by customer.");
            return false;
        }*/
        if(savingsGoalEntity.getCreationSource() == SavingsGoalCreationSourceConstant.MINT && savingsGoalEntity.getSavingsBalance().compareTo(BigDecimal.valueOf(50000.0)) >= 0){
            log.info("Interest cannot be applied to mint savings goal above 50k.");
            return false;
        }
        if(savingsGoalEntity.getGoalStatus() != SavingsGoalStatusConstant.ACTIVE) {
            log.info("Saving goal is not longer active.");
            return false;
        }
        if(savingsGoalEntity.getLastInterestApplicationDate() != null) {
            boolean interestAppliedToday = DateUtil.sameDay(LocalDateTime.now(), savingsGoalEntity.getLastInterestApplicationDate());
            log.info("Interest has been applied today: {}", interestAppliedToday);
            return !interestAppliedToday;
        }else {
            if(savingsInterestEntityDao.countInterestOnGoal(savingsGoalEntity) == 0) {
                return true;
            }
            log.info("This is weird. Savings interest exist but LastInterestApplicationDate is null. Hmmmmm.. Skipped.");
        }
        return false;
    }

    public void updateInterestLiabilityAccountWithAccumulatedInterest(BigDecimal totalAccumulatedInterest) {
        if(totalAccumulatedInterest.compareTo(BigDecimal.ZERO) == 0) {
            log.info("NO ACCUMULATED INTEREST: {}", totalAccumulatedInterest);
            return;
        }

        String reference = accumulatedInterestEntityDao.generatedReference();
        AccumulatedInterestEntity accumulatedInterestEntity = AccumulatedInterestEntity.builder()
                .interestDate(LocalDate.now()).totalInterest(totalAccumulatedInterest)
                .reference(reference).transactionStatus(TransactionStatusConstant.PENDING)
                .build();
        accumulatedInterestEntityDao.saveRecord(accumulatedInterestEntity);

        String narration = "SGAI - "+reference+" Accumulated Interest";
        InterestAccruedUpdateRequestCBS updateRequestCBS = InterestAccruedUpdateRequestCBS.builder()
                .interestAmount(totalAccumulatedInterest)
                .reference(accumulatedInterestEntity.getReference())
                .narration(narration)
                .build();
        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.updateAccruedInterest(updateRequestCBS);
        if(msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
            String message = msClientResponse.getMessage();
            accumulatedInterestEntity.setTransactionStatus(TransactionStatusConstant.FAILED);
            accumulatedInterestEntity.setResponseMessage(message);
            accumulatedInterestEntityDao.saveRecord(accumulatedInterestEntity);
            systemIssueLogService.logIssue("Accumulated Interest Update Failure", reference+" - "+message);
            return;
        }
        FundTransferResponseCBS responseCBS = msClientResponse.getData();
        if(!"00".equalsIgnoreCase(responseCBS.getResponseCode())) {
            accumulatedInterestEntity.setTransactionStatus(TransactionStatusConstant.FAILED);
            accumulatedInterestEntity.setResponseMessage(responseCBS.getResponseMessage());
            accumulatedInterestEntity.setResponseCode(responseCBS.getResponseCode());
            accumulatedInterestEntityDao.saveRecord(accumulatedInterestEntity);
            systemIssueLogService.logIssue("Accumulated Interest Update Failure", reference+" - "+responseCBS.getResponseMessage());
            return;
        }
        accumulatedInterestEntity.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);
        accumulatedInterestEntity.setResponseMessage(responseCBS.getResponseMessage());
        accumulatedInterestEntity.setResponseCode(responseCBS.getResponseCode());
        accumulatedInterestEntity.setExternalReference(responseCBS.getBankOneReference());
        accumulatedInterestEntityDao.saveRecord(accumulatedInterestEntity);
    }

    private void publishInterestApplication(SavingsGoalEntity savingsGoalEntity) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getRecordById(savingsGoalEntity.getMintAccount().getId());
        SavingsGoalBalanceUpdateEvent updateEvent = SavingsGoalBalanceUpdateEvent.builder()
                .goalId(savingsGoalEntity.getGoalId())
                .accountId(accountEntity.getAccountId())
                .accruedInterest(savingsGoalEntity.getAccruedInterest())
                .savingsBalance(savingsGoalEntity.getSavingsBalance())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.SAVING_GOAL_BALANCE_UPDATE, new EventModel<>(updateEvent));
    }
}
