package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentInterestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface InvestmentInterestRepository extends JpaRepository<InvestmentInterestEntity, Long> {

    @Query("select sum(i.interest) from InvestmentInterestEntity i where i.investment =:investment")
    Optional<BigDecimal> sumInvestmentInterest(@Param("investment") InvestmentEntity investment);


}
