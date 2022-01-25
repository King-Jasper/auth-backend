package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.SpendAndSaveTransactionEntity;
import com.mintfintech.savingsms.domain.dao.SpendAndSaveTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SpendAndSaveTransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.inject.Named;
import java.util.Optional;

@Named
@AllArgsConstructor
public class SpendAndSaveTransactionEntityDaoImpl implements SpendAndSaveTransactionEntityDao {

    private final SpendAndSaveTransactionRepository repository;

    @Override
    public Optional<SpendAndSaveTransactionEntity> findById(Long aLong) {
        return Optional.empty();
    }

    @Override
    public SpendAndSaveTransactionEntity getRecordById(Long aLong) throws RuntimeException {
        return null;
    }

    @Override
    public SpendAndSaveTransactionEntity saveRecord(SpendAndSaveTransactionEntity record) {
        return null;
    }

    @Override
    public Optional<SpendAndSaveTransactionEntity> findByTransactionReference(String reference) {
        return repository.findTopByTransactionReference(reference);
    }

    @Override
    public Page<SpendAndSaveTransactionEntity> getTransactionsBySavingsGoal(SavingsGoalEntity savingsGoal, int pageIndex, int size) {
        Pageable pageable = PageRequest.of(pageIndex, size);
        return repository.getTransactionsBySavingsGoal(savingsGoal, pageable);
    }
}
