package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.InvestmentTenorEntity;

import java.util.List;
import java.util.Optional;

public interface InvestmentTenorEntityDao extends CrudDao<InvestmentTenorEntity, Long>{

    Optional<InvestmentTenorEntity> findInvestmentTenor(int minimumDuration, int maximumDuration);
    List<InvestmentTenorEntity> getTenorList();
}
