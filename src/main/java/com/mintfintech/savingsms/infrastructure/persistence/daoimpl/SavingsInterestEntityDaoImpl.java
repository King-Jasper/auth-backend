package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.SavingsInterestEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsInterestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsInterestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@Named
public class SavingsInterestEntityDaoImpl implements SavingsInterestEntityDao {

    private SavingsInterestRepository repository;
    public SavingsInterestEntityDaoImpl(SavingsInterestRepository repository) {
        this.repository = repository;
    }

    @Override
    public BigDecimal getTotalInterestAmountOnGoal(SavingsGoalEntity savingsGoalEntity) {
        return repository.sumSavingsInterest(savingsGoalEntity).orElse(BigDecimal.ZERO);
    }

    @Override
    public long countInterestOnGoal(SavingsGoalEntity savingsGoalEntity) {
        return repository.countAllBySavingsGoal(savingsGoalEntity);
    }

    @Override
    public long countInterestOnGoal(SavingsGoalEntity savingsGoalEntity, LocalDateTime fromTime, LocalDateTime toTime) {
        return repository.countAllBySavingsGoalAndDateCreatedBetween(savingsGoalEntity, fromTime, toTime);
    }

    @Override
    public Optional<SavingsInterestEntity> findLastInterestApplied(SavingsGoalEntity savingsGoalEntity) {
        return repository.findFirstBySavingsGoalOrderByDateCreatedDesc(savingsGoalEntity);
    }

    @Override
    public Optional<SavingsInterestEntity> findFirstInterestApplied(SavingsGoalEntity savingsGoalEntity) {
        return repository.findFirstBySavingsGoalOrderByDateCreatedAsc(savingsGoalEntity);
    }

    @Override
    public Page<SavingsInterestEntity> getAccruedInterestOnGoal(SavingsGoalEntity goalEntity, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.getAllByRecordStatusAndSavingsGoalOrderByDateCreatedDesc(RecordStatusConstant.ACTIVE, goalEntity, pageable);
    }

    @Override
    public List<SavingsInterestEntity> getSavingsGoalInterest(SavingsGoalEntity goalEntity) {



        return repository.getAllByRecordStatusAndSavingsGoalOrderByDateCreated(RecordStatusConstant.ACTIVE, goalEntity);
    }

    @Override
    public Optional<SavingsInterestEntity> getSavingsInterestOnDate(SavingsGoalEntity savingsGoal, LocalDate interestDate) {
        String interestDateString = interestDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<SavingsInterestEntity> list = repository.getInterestOnDate(savingsGoal, interestDateString);
        if(list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    @Override
    public Optional<SavingsInterestEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public SavingsInterestEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. SavingsInterestEntity with Id: "+aLong));
    }

    @Override
    public SavingsInterestEntity saveRecord(SavingsInterestEntity record) {
        return repository.save(record);
    }
}
