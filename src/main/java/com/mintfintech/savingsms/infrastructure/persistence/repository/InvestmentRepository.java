package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 17 May, 2021
 */
public interface InvestmentRepository extends JpaRepository<InvestmentEntity, Long> {

    @Query("select count(i) from InvestmentEntity i where i.amountInvested > 0.0 and i.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            "and i.investmentStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE")
    long countEligibleInterestInvestment();

    @Query("select i from InvestmentEntity i where i.amountInvested > 0.0 and i.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            "and i.investmentStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE")
    Page<InvestmentEntity> getEligibleInterestInvestment(Pageable pageable);


    @Query(value = "select i from InvestmentEntity i where i.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE" +
            " and i.investmentStatus =:status and i.maturityDate between :fromTime and :toTime")
    Page<InvestmentEntity> getInvestmentWithMaturityPeriod(@Param("status") SavingsGoalStatusConstant status,
                                                             @Param("fromTime") LocalDateTime fromTime,
                                                             @Param("toTime") LocalDateTime toTime, Pageable pageable);

    Optional<InvestmentEntity> findTopByCodeIgnoreCase(String code);

    List<InvestmentEntity> getAllByOwnerAndRecordStatus(MintAccountEntity accountEntity, RecordStatusConstant statusConstant);



}
