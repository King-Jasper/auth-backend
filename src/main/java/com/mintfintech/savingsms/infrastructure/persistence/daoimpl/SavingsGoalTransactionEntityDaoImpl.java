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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.StaleObjectStateException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import javax.inject.Named;
import javax.persistence.LockTimeoutException;
import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 31 Mar, 2020
 */
@Slf4j
@Named
public class SavingsGoalTransactionEntityDaoImpl extends CrudDaoImpl<SavingsGoalTransactionEntity, Long> implements SavingsGoalTransactionEntityDao {
    private final SavingsGoalTransactionRepository repository;
    private final AppSequenceEntityDao appSequenceEntityDao;

    public SavingsGoalTransactionEntityDaoImpl(SavingsGoalTransactionRepository repository, AppSequenceEntityDao appSequenceEntityDao) {
        super(repository);
        this.repository = repository;
        this.appSequenceEntityDao = appSequenceEntityDao;
    }


    @Override
    public Optional<SavingsGoalTransactionEntity> findTransactionByReference(String transactionReference) {
        return repository.findFirstByTransactionReference(transactionReference);
    }

    /*
    @Override
    public String generateTransactionReference() {
        return String.format("MS%09d%s", appSequenceEntityDao.getNextSequenceId(SequenceType.SAVING_GOAL_REFERENCE_SEQ), RandomStringUtils.randomNumeric(1));
    }
    */

    @Override
    public String generateTransactionReference() {
        int retries = 0;
        boolean success = false;
        String reference = RandomStringUtils.random(8);
        while(!success && retries < 5) {
            try {
                reference = String.format("MS%09d%s", appSequenceEntityDao.getNextSequenceIdTemp(SequenceType.SAVING_GOAL_REFERENCE_SEQ), RandomStringUtils.randomNumeric(1));
                success = true;
            }catch (StaleObjectStateException | ObjectOptimisticLockingFailureException | OptimisticLockException | LockTimeoutException ex){
                log.info("savings-exception caught - {},  reference - {}, retries - {}", ex.getClass().getSimpleName(), reference, retries);
                retries++;
                success = false;
            }
            if(retries > 0 && success) {
                log.info("Successful retrieval of unique reference Id - {}", reference);
            }
        }
        if(retries >= 5) {
            reference = "MS"+RandomStringUtils.random(10);
        }
        return reference;
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
