package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.InvestmentWithdrawalEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentWithdrawalEntity;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentWithdrawalStageConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.InvestmentWithdrawalRepository;
import javax.inject.Named;
import java.util.List;

/**
 * Created by jnwanya on
 * Thu, 20 May, 2021
 */
@Named
public class InvestmentWithdrawalEntityDaoImpl extends CrudDaoImpl<InvestmentWithdrawalEntity, Long> implements InvestmentWithdrawalEntityDao {

    private final InvestmentWithdrawalRepository repository;
    public InvestmentWithdrawalEntityDaoImpl(InvestmentWithdrawalRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public List<InvestmentWithdrawalEntity> getWithdrawalByInvestmentAndStatus(InvestmentEntity investmentEntity, InvestmentWithdrawalStageConstant withdrawalStatus) {
        return repository.getAllByInvestmentAndWithdrawalStage(investmentEntity, withdrawalStatus);
    }
}
