package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.AccumulatedInterestEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentInterestEntityDao;
import com.mintfintech.savingsms.domain.entities.AccumulatedInterestEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentInterestEntity;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.InterestCategoryConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.InterestAccruedUpdateRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.usecase.features.investment.ApplyInvestmentInterestUseCase;
import com.mintfintech.savingsms.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApplyInvestmentInterestUseCaseImpl implements ApplyInvestmentInterestUseCase {

    private final InvestmentEntityDao investmentEntityDao;
    private final InvestmentInterestEntityDao investmentInterestEntityDao;
    private final CoreBankingServiceClient coreBankingServiceClient;
    private final SystemIssueLogService systemIssueLogService;
    private final AccumulatedInterestEntityDao accumulatedInterestEntityDao;

    @Override
    public void processAndApplyInterest() {

        int size = 1000;
        BigDecimal totalAccumulatedInterest = BigDecimal.valueOf(0.00);

        Page<InvestmentEntity> pagedInvestments = investmentEntityDao.getRecordsForEligibleInterestApplication(0, size);

        totalAccumulatedInterest = totalAccumulatedInterest.add(processInterestComputation(pagedInvestments.getContent()));

        int totalPages = pagedInvestments.getTotalPages();

        if (pagedInvestments.getTotalElements() > 0) {
            log.info("Investments for interest consideration: {}", pagedInvestments.getTotalElements());
        }
        for (int i = 1; i < totalPages; i++) {
            pagedInvestments = investmentEntityDao.getRecordsForEligibleInterestApplication(i, size);
            totalAccumulatedInterest = totalAccumulatedInterest.add(processInterestComputation(pagedInvestments.getContent()));
        }
        updateInterestLiabilityAccountWithAccumulatedInterest(totalAccumulatedInterest);

    }

    private BigDecimal processInterestComputation(List<InvestmentEntity> investments) {
        BigDecimal totalInterest = BigDecimal.valueOf(0.0);
        for (InvestmentEntity investment : investments) {
            try {
                if (!shouldApplyInterest(investment)) {
                    continue;
                }
                BigDecimal interest = applyInterest(investment);
                totalInterest = totalInterest.add(interest);
            } catch (Exception ignored) {
            }
        }
        return totalInterest;
    }

    private BigDecimal applyInterest(InvestmentEntity investment) {

        double interestRatePerDay = investment.getInterestRate() / (100.0 * 365.0);
        BigDecimal interest = investment.getAmountInvested().multiply(BigDecimal.valueOf(interestRatePerDay)).setScale(2, BigDecimal.ROUND_HALF_EVEN);

        InvestmentInterestEntity investmentInterest = new InvestmentInterestEntity();
        investmentInterest.setInvestment(investment);
        investmentInterest.setInterest(interest);
        investmentInterest.setRate(investment.getInterestRate());
        investmentInterest.setSavingsBalance(investment.getAmountInvested());
        investmentInterestEntityDao.saveRecord(investmentInterest);

        investment.setAccruedInterest(investment.getAccruedInterest().add(interest));
        investment.setLastInterestApplicationDate(LocalDateTime.now());
        investmentEntityDao.saveRecord(investment);

        return interest;
    }

    private boolean shouldApplyInterest(InvestmentEntity investment) {

        if (investment.getInvestmentStatus() != InvestmentStatusConstant.ACTIVE) {
            log.info("Investment is not longer active.");
            return false;
        }

        if (investment.getLastInterestApplicationDate() != null) {
            boolean interestAppliedToday = DateUtil.sameDay(LocalDateTime.now(), investment.getLastInterestApplicationDate());
            log.info("Interest has been applied today: {}", interestAppliedToday);
            return !interestAppliedToday;
        }

        return true;
    }

    private void updateInterestLiabilityAccountWithAccumulatedInterest(BigDecimal totalAccumulatedInterest) {
        if (totalAccumulatedInterest.compareTo(BigDecimal.ZERO) == 0) {
            log.info("NO ACCUMULATED INTEREST: {}", totalAccumulatedInterest);
            return;
        }
        String reference = accumulatedInterestEntityDao.generatedReference();
        AccumulatedInterestEntity accumulatedInterestEntity = AccumulatedInterestEntity.builder()
                .interestDate(LocalDate.now())
                .totalInterest(totalAccumulatedInterest)
                .reference(reference)
                .transactionStatus(TransactionStatusConstant.PENDING)
                .interestCategory(InterestCategoryConstant.INVESTMENT)
                .build();
        accumulatedInterestEntityDao.saveRecord(accumulatedInterestEntity);
        processInterestPostingOnCBA(accumulatedInterestEntity);
    }

    private void processInterestPostingOnCBA(AccumulatedInterestEntity accumulatedInterestEntity) {

        String reference = accumulatedInterestEntity.getReference();
        String narration = "IAI - " + reference + " Accumulated Interest";

        InterestAccruedUpdateRequestCBS updateRequestCBS = InterestAccruedUpdateRequestCBS.builder()
                .interestAmount(accumulatedInterestEntity.getTotalInterest())
                .reference(accumulatedInterestEntity.getReference())
                .narration(narration)
                .interestCategory(InterestCategoryConstant.INVESTMENT.name())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.updateInvestmentAccruedInterest(updateRequestCBS);

        if (msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
            String message = msClientResponse.getMessage();
            accumulatedInterestEntity.setTransactionStatus(TransactionStatusConstant.FAILED);
            accumulatedInterestEntity.setResponseMessage(message);
            accumulatedInterestEntityDao.saveRecord(accumulatedInterestEntity);
            systemIssueLogService.logIssue("Interest Posting Failed", "Accumulated Interest Update Failure", reference + " - " + message);
            return;
        }
        FundTransferResponseCBS responseCBS = msClientResponse.getData();
        if (!"00".equalsIgnoreCase(responseCBS.getResponseCode())) {
            accumulatedInterestEntity.setTransactionStatus(TransactionStatusConstant.FAILED);
            accumulatedInterestEntity.setResponseMessage(responseCBS.getResponseMessage());
            accumulatedInterestEntity.setResponseCode(responseCBS.getResponseCode());
            accumulatedInterestEntityDao.saveRecord(accumulatedInterestEntity);
            systemIssueLogService.logIssue("Interest Posting Failed", "Accumulated Interest Update Failure", reference + " - " + responseCBS.getResponseMessage());
            return;
        }
        accumulatedInterestEntity.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);
        accumulatedInterestEntity.setResponseMessage(responseCBS.getResponseMessage());
        accumulatedInterestEntity.setResponseCode(responseCBS.getResponseCode());
        accumulatedInterestEntity.setExternalReference(responseCBS.getBankOneReference());
        accumulatedInterestEntityDao.saveRecord(accumulatedInterestEntity);
    }

    @Override
    public void processFailedInterestPosting() {
         LocalDate yesterday = LocalDate.now().minusDays(1);

         LocalDateTime fromTime = yesterday.atStartOfDay();
         LocalDateTime toTime = yesterday.atTime(LocalTime.MAX);

         List<AccumulatedInterestEntity> failedInterests = accumulatedInterestEntityDao.getFailedInterestRecord(fromTime, toTime);
         for(AccumulatedInterestEntity failedInterest: failedInterests) {
             String code = StringUtils.defaultString(failedInterest.getResponseCode());
             if(code.equalsIgnoreCase("91")) {
                 continue;
             }
             failedInterest.setReference(accumulatedInterestEntityDao.generatedReference());
             accumulatedInterestEntityDao.saveRecord(failedInterest);
             processInterestPostingOnCBA(failedInterest);
         }
    }
}
