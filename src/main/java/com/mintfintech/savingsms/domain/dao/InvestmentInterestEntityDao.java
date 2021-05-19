package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentInterestEntity;

import java.math.BigDecimal;

public interface InvestmentInterestEntityDao extends CrudDao<InvestmentInterestEntity, Long>{

    BigDecimal getTotalInterestAmountOnInvestment(InvestmentEntity investmentEntity);
}
