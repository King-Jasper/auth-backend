package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentWithdrawalEntity;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentWithdrawalStageConstant;

import java.util.List;

/**
 * Created by jnwanya on
 * Thu, 20 May, 2021
 */
public interface InvestmentWithdrawalEntityDao extends CrudDao<InvestmentWithdrawalEntity, Long>{

    List<InvestmentWithdrawalEntity> getWithdrawalByInvestmentAndStatus(InvestmentEntity investmentEntity, InvestmentWithdrawalStageConstant withdrawalStatus);

    List<InvestmentWithdrawalEntity> getWithdrawalByInvestmentAndStatus(InvestmentWithdrawalStageConstant withdrawalStatus);
}
