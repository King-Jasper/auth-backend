package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsWithdrawalRequestEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsWithdrawalRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsWithdrawalRequestRepository;
import org.apache.commons.lang3.RandomStringUtils;

import javax.inject.Named;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
@Named
public class SavingsWithdrawalRequestEntityDaoImpl implements SavingsWithdrawalRequestEntityDao {

    private SavingsWithdrawalRequestRepository repository;
    private AppSequenceEntityDao appSequenceEntityDao;
    public SavingsWithdrawalRequestEntityDaoImpl(SavingsWithdrawalRequestRepository repository, AppSequenceEntityDao appSequenceEntityDao) {
        this.repository = repository;
        this.appSequenceEntityDao = appSequenceEntityDao;
    }

    @Override
    public String generateInterestTransactionReference() {
        return String.format("MI%09d%s", appSequenceEntityDao.getNextSequenceId(SequenceType.SAVING_INTEREST_REFERENCE_SEQ), RandomStringUtils.randomNumeric(1));
    }

    @Override
    public long countWithdrawalRequestWithinPeriod(SavingsGoalEntity savingsGoal, LocalDateTime fromTime, LocalDateTime toTime) {
        return repository.countAllBySavingsGoalAndDateCreatedBetween(savingsGoal, fromTime, toTime);
    }

    @Override
    public List<SavingsWithdrawalRequestEntity> getSavingsWithdrawalByStatus(WithdrawalRequestStatusConstant withdrawalRequestStatusConstant) {
        LocalDate now = LocalDate.now();
        return repository.getSavingsWithdrawalRequest(RecordStatusConstant.ACTIVE, withdrawalRequestStatusConstant, now);
    }

    @Override
    public Optional<SavingsWithdrawalRequestEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public SavingsWithdrawalRequestEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. SavingsWithdrawalRequestEntity with Id: "+aLong));
    }

    @Override
    public SavingsWithdrawalRequestEntity saveRecord(SavingsWithdrawalRequestEntity record) {
        return repository.save(record);
    }

    @Override
    public SavingsWithdrawalRequestEntity saveAndFlush(SavingsWithdrawalRequestEntity savingsWithdrawalRequestEntity) {
        return repository.saveAndFlush(savingsWithdrawalRequestEntity);
    }
}
