package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.CorporateTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionEntity;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionTypeConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.CorporateTransactionRepository;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;

import javax.inject.Named;
import java.util.Optional;

@Named
public class CorporateTransactionEntityDaoImpl extends CrudDaoImpl<CorporateTransactionEntity, Long> implements CorporateTransactionEntityDao {

    private final CorporateTransactionRepository repository;
    public CorporateTransactionEntityDaoImpl(CorporateTransactionRepository repository) {
        super(repository);
        this.repository = repository;
    }

    public Optional<CorporateTransactionEntity> findByTransactionRequest(CorporateTransactionRequestEntity requestEntity) {
        return repository.findByTransactionRequest(requestEntity);
    }

    @Override
    public CorporateTransactionEntity getByTransactionRequest(CorporateTransactionRequestEntity requestEntity) {
        return findByTransactionRequest(requestEntity).orElseThrow(() -> new BusinessLogicConflictException("Transaction not found"));
    }

    public Optional<CorporateTransactionEntity> findByTransactionRequestAndTransactionType(CorporateTransactionRequestEntity requestEntity, CorporateTransactionTypeConstant transactionType) {
        return repository.findByTransactionRequestAndTransactionType(requestEntity, transactionType);
    }

    @Override
    public CorporateTransactionEntity getByTransactionRequestAndTransactionType(CorporateTransactionRequestEntity requestEntity, CorporateTransactionTypeConstant transactionType) {
        return findByTransactionRequestAndTransactionType(requestEntity, transactionType).orElseThrow(() -> new BusinessLogicConflictException("Transaction not found"));
    }
}
