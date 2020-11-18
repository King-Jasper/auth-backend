package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsGoalTransactionRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.inject.Named;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 31 Mar, 2020
 */
@AllArgsConstructor
@Named
public class SavingsGoalTransactionEntityDaoImpl implements SavingsGoalTransactionEntityDao {
    private SavingsGoalTransactionRepository repository;
    private AppSequenceEntityDao appSequenceEntityDao;

    @Override
    public Optional<SavingsGoalTransactionEntity> findTransactionByReference(String transactionReference) {
        return repository.findFirstByTransactionReference(transactionReference);
    }

    @Override
    public String generateTransactionReference() {
        return String.format("MS%09d%s", appSequenceEntityDao.getNextSequenceId(SequenceType.SAVING_GOAL_REFERENCE_SEQ), RandomStringUtils.randomNumeric(1));
    }

    @Override
    public Optional<SavingsGoalTransactionEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public SavingsGoalTransactionEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. SavingsGoalTransactionEntity with Id: "+aLong));
    }

    @Override
    public List<SavingsGoalTransactionEntity> getTransactionByTypeAndStatusBeforeTime(TransactionTypeConstant transactionType, TransactionStatusConstant transactionStatus, LocalDateTime beforeTime, int size) {
        Pageable pageable = PageRequest.of(0, size);
        return repository.getAllByRecordStatusAndTransactionTypeAndTransactionStatusAndDateCreatedBefore(RecordStatusConstant.ACTIVE, transactionType, transactionStatus, beforeTime, pageable);
    }

    @Override
    public Page<SavingsGoalTransactionEntity> getTransactions(SavingsGoalEntity goalEntity, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.getAllByRecordStatusAndSavingsGoalOrderByDateCreatedDesc(RecordStatusConstant.ACTIVE, goalEntity, pageable);
    }

    @Override
    public SavingsGoalTransactionEntity saveRecord(SavingsGoalTransactionEntity record) {
        return repository.save(record);
    }
}
