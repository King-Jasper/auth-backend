package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.RoundUpSavingsTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.RoundUpSavingsTransactionEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.RoundUpSavingsTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Override
    public Page<RoundUpSavingsTransactionEntity> getSuccessfulTransactionOnGoal(SavingsGoalEntity savingsGoalEntity, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.getSuccessfulTransactions(savingsGoalEntity, pageable);
    }
}
