package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentWithdrawalEntity;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentWithdrawalStageConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by jnwanya on
 * Thu, 20 May, 2021
 */
public interface InvestmentWithdrawalRepository extends JpaRepository<InvestmentWithdrawalEntity, Long> {

    List<InvestmentWithdrawalEntity> getAllByInvestmentAndWithdrawalStatus(InvestmentEntity investment, InvestmentWithdrawalStageConstant withdrawalStatus);

}
