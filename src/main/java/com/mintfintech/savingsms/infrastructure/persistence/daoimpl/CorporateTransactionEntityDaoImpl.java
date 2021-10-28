package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.CorporateTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionEntity;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.CorporateTransactionRepository;

import javax.inject.Named;

@Named
public class CorporateTransactionEntityDaoImpl extends CrudDaoImpl<CorporateTransactionEntity, Long> implements CorporateTransactionEntityDao {

    private final CorporateTransactionRepository repository;
    public CorporateTransactionEntityDaoImpl(CorporateTransactionRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public CorporateTransactionEntity getByTransactionRequest(CorporateTransactionRequestEntity requestEntity) {
        return repository.getByTransactionRequest(requestEntity);
    }
}
