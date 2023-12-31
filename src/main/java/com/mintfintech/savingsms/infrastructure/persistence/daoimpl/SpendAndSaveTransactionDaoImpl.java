package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.SpendAndSaveTransactionDao;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SpendAndSaveTransactionEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SpendAndSaveTransactionRepository;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.inject.Named;
import java.util.Optional;

@Named
@AllArgsConstructor
public class SpendAndSaveTransactionDaoImpl implements SpendAndSaveTransactionDao {

    private final SpendAndSaveTransactionRepository repository;

    @Override
    public Optional<SpendAndSaveTransactionEntity> findByTransactionReference(String reference) {
        return repository.findTopByTransactionReference(reference);
    }

    @Override
    public Page<SpendAndSaveTransactionEntity> getTransactionsBySavingsGoal(SavingsGoalEntity savingsGoal, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.getTransactionsBySavingsGoal(savingsGoal, pageable);
    }

    @Override
    public Optional<SpendAndSaveTransactionEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public SpendAndSaveTransactionEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new BadRequestException("Record with id "+ aLong+ " not found"));
    }

    @Override
    public SpendAndSaveTransactionEntity saveRecord(SpendAndSaveTransactionEntity record) {
        return repository.saveAndFlush(record);
    }
}
