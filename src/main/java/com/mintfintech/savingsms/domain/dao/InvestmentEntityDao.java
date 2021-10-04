package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.models.InvestmentSearchDTO;
import com.mintfintech.savingsms.domain.models.reports.InvestmentStat;
import com.mintfintech.savingsms.domain.models.reports.SavingsMaturityStat;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 17 May, 2021
 */
public interface InvestmentEntityDao extends CrudDao<InvestmentEntity, Long> {
    String generateCode();

    List<InvestmentEntity> getRecordsOnAccount(MintAccountEntity mintAccountEntity);

    Optional<InvestmentEntity> findByCode(String code);

    Page<InvestmentEntity> searchInvestments(InvestmentSearchDTO investmentSearchDTO, int pageIndex, int recordSize);

    BigDecimal sumSearchedInvestments(InvestmentSearchDTO investmentSearchDTO);

    Page<InvestmentEntity> getRecordsForEligibleInterestApplication(int pageIndex, int recordSize);

    Page<InvestmentEntity> getRecordsWithMaturityDateWithinPeriod(LocalDateTime fromTime, LocalDateTime toTime, int pageIndex, int recordSize);

    List<InvestmentStat> getInvestmentStatOnAccount(MintAccountEntity mintAccountEntity);

    List<InvestmentStat> getStatsForCompletedInvestment(MintAccountEntity mintAccountEntity);

    List<SavingsMaturityStat> getInvestmentMaturityStatistics(LocalDateTime fromDate, LocalDateTime toDate);

}
