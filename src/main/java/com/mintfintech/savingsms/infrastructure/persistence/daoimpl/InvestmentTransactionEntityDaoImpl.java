package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.InvestmentTransactionRepository;
import org.apache.commons.lang3.RandomStringUtils;

import javax.inject.Named;
import java.util.List;

@Named
public class InvestmentTransactionEntityDaoImpl extends CrudDaoImpl<InvestmentTransactionEntity, Long> implements InvestmentTransactionEntityDao {

    private final InvestmentTransactionRepository repository;
    private final AppSequenceEntityDao appSequenceEntityDao;
    public InvestmentTransactionEntityDaoImpl(InvestmentTransactionRepository repository, AppSequenceEntityDao appSequenceEntityDao) {
        super(repository);
        this.repository = repository;
        this.appSequenceEntityDao = appSequenceEntityDao;
    }

    @Override
    public String generateTransactionReference() {
        return String.format("MI%09d%s", appSequenceEntityDao.getNextSequenceId(SequenceType.INVESTMENT_TRANSACTION_REFERENCE_SEQ), RandomStringUtils.randomNumeric(1));
    }

    @Override
    public List<InvestmentTransactionEntity> getTransactionsByInvestment(InvestmentEntity investmentEntity, TransactionTypeConstant type, TransactionStatusConstant status) {
        return repository.getAllByInvestmentAndTransactionTypeAndTransactionStatus(investmentEntity, type, status);
    }

    @Override
    public List<InvestmentTransactionEntity> getTransactionsByInvestment(InvestmentEntity investmentEntity) {
        return repository.getAllByRecordStatusAndInvestment(RecordStatusConstant.ACTIVE, investmentEntity);
    }
}
