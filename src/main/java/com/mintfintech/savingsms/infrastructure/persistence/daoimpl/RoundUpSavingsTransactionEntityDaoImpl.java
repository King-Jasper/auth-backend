package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.RoundUpSavingsTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.RoundUpSavingsTransactionEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.RoundUpSavingsTransactionRepository;
import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
@Named
public class RoundUpSavingsTransactionEntityDaoImpl extends CrudDaoImpl<RoundUpSavingsTransactionEntity, Long> implements RoundUpSavingsTransactionEntityDao {

    private final RoundUpSavingsTransactionRepository repository;
    public RoundUpSavingsTransactionEntityDaoImpl(RoundUpSavingsTransactionRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Optional<RoundUpSavingsTransactionEntity> findByTransactionReference(String reference) {
        return repository.findTopByTransactionReference(reference);
    }
}
