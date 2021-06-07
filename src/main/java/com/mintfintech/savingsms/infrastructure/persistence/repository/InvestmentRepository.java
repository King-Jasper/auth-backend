package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.models.reports.InvestmentStat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 17 May, 2021
 */
public interface InvestmentRepository extends JpaRepository<InvestmentEntity, Long>, JpaSpecificationExecutor<InvestmentEntity> {

    @Query("select count(i) from InvestmentEntity i where i.amountInvested > 0.0 and i.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            "and i.investmentStatus = com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant.ACTIVE")
    long countEligibleInterestInvestment();

    @Query("select i from InvestmentEntity i where i.amountInvested > 0.0 and i.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            "and i.investmentStatus = com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant.ACTIVE")
    Page<InvestmentEntity> getEligibleInterestInvestment(Pageable pageable);


    @Query(value = "select i from InvestmentEntity i where i.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE" +
            " and i.investmentStatus =:status and i.maturityDate between :fromTime and :toTime order by i.maturityDate asc")
    Page<InvestmentEntity> getInvestmentWithMaturityPeriod(@Param("status") InvestmentStatusConstant status,
                                                           @Param("fromTime") LocalDateTime fromTime,
                                                           @Param("toTime") LocalDateTime toTime, Pageable pageable);

    Optional<InvestmentEntity> findTopByCodeIgnoreCase(String code);

    List<InvestmentEntity> getAllByOwnerAndRecordStatus(MintAccountEntity accountEntity, RecordStatusConstant statusConstant);

    @Query(value = "select new com.mintfintech.savingsms.domain.models.reports.InvestmentStat(i.investmentStatus, count(i), sum(i.amountInvested), sum(i.accruedInterest), sum(i.accruedInterest), sum(((i.interestRate * 0.01 * i.amountInvested)/365.0) * DAY(i.maturityDate - NOW()))) " +
            "from InvestmentEntity i where " +
            "i.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and " +
            "i.owner =:owner " +
            "group by i.investmentStatus")
    List<InvestmentStat> getInvestmentStatistics(@Param("owner") MintAccountEntity accountEntity);

    @Query(value = "select new com.mintfintech.savingsms.domain.models.reports.InvestmentStat(i.investmentStatus, count(i), sum(i.totalAmountInvested), sum(i.accruedInterest), sum(i.totalAmountWithdrawn), 0.0) " +
            "from InvestmentEntity i where " +
            "i.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and " +
            "i.owner =:owner " +
            "group by i.investmentStatus")
    List<InvestmentStat> getStatisticsForCompletedInvestment(@Param("owner") MintAccountEntity accountEntity);
}
