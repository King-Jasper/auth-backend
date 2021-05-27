package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.InvestmentEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentWithdrawalEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentWithdrawalEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentWithdrawalStageConstant;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentWithdrawalTypeConstant;
import com.mintfintech.savingsms.usecase.features.investment.UpdateInvestmentMaturityUseCase;
import com.mintfintech.savingsms.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class UpdateInvestmentMaturityUseCaseImpl implements UpdateInvestmentMaturityUseCase {

    private final InvestmentEntityDao investmentEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final InvestmentWithdrawalEntityDao investmentWithdrawalEntityDao;

    @Override
    public void updateStatusForMaturedInvestment() {

        /*

        LocalDateTime startTime, endTime;
        if (now.getHour() > 12) {
            startTime = now.withHour(12).withMinute(0);
            endTime = LocalDateTime.of(LocalDate.now(), LocalTime.MAX).withNano(0);
        } else {
            startTime = LocalDate.now().atStartOfDay();
            endTime = now.withHour(12).withMinute(0);
        }
        */
       // LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime =  LocalDate.now().atTime(LocalTime.MAX);

        int size = 20;
        Page<InvestmentEntity> pagedResponse = investmentEntityDao.getRecordsWithMaturityDateWithinPeriod(startTime, endTime, 0, size);

        processMaturityStatusUpdate(pagedResponse.getContent());

        int totalPages = pagedResponse.getTotalPages();
        if (pagedResponse.getTotalElements() > 0) {
            log.info("Investment for interest consideration: {}", pagedResponse.getTotalElements());
        }
        for (int i = 1; i < totalPages; i++) {
            pagedResponse = investmentEntityDao.getRecordsWithMaturityDateWithinPeriod(startTime, endTime, i, size);
            processMaturityStatusUpdate(pagedResponse.getContent());
        }
    }

    private void processMaturityStatusUpdate(List<InvestmentEntity> investments) {

        for (InvestmentEntity investment : investments) {
            long remainingDays = LocalDateTime.now().until(investment.getMaturityDate(), ChronoUnit.DAYS);

            if (remainingDays > 0) {
                log.info("Investment :{} not yet matured: {}", investment.getCode(), investment.getMaturityDate().toString());
                continue;
            }

            if (investment.getInvestmentStatus() != InvestmentStatusConstant.ACTIVE) {
                log.info("Investment :{} is not active", investment.getCode());
                continue;
            }
            investment.setInvestmentStatus(InvestmentStatusConstant.COMPLETED);
            investment.setDateWithdrawn(LocalDateTime.now());
            investmentEntityDao.saveRecord(investment);

            MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(investment.getOwner(), BankAccountTypeConstant.CURRENT);

            BigDecimal amountInvested = investment.getAmountInvested();
            BigDecimal accruedInterest = investment.getAccruedInterest();
            BigDecimal amountToWithdraw = amountInvested.add(accruedInterest);

            InvestmentWithdrawalEntity withdrawalEntity = InvestmentWithdrawalEntity.builder()
                    .amount(amountToWithdraw)
                    .balanceBeforeWithdrawal(amountInvested)
                    .interestBeforeWithdrawal(accruedInterest)
                    .dateForWithdrawal(DateUtil.addWorkingDays(LocalDate.now(), 2))
                    .interest(accruedInterest)
                    .investment(investment)
                    .matured(true)
                    .creditAccount(creditAccount)
                    .withdrawalStage(InvestmentWithdrawalStageConstant.PENDING_INTEREST_TO_CUSTOMER)
                    .withdrawalType(InvestmentWithdrawalTypeConstant.MATURITY_WITHDRAWAL)
                    .requestedBy(investment.getCreator())
                    .totalAmount(amountToWithdraw)
                    .build();

            investmentWithdrawalEntityDao.saveRecord(withdrawalEntity);
        }
    }
}
