package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.SavingsGoalCategoryEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsGoalCategoryEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsGoalCategoryRepository;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Wed, 01 Apr, 2020
 */
@Named
public class SavingsGoalCategoryEntityDaoImpl implements SavingsGoalCategoryEntityDao {

    private SavingsGoalCategoryRepository repository;
    public SavingsGoalCategoryEntityDaoImpl(SavingsGoalCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SavingsGoalCategoryEntity> findCategoryByCode(String code) {
        return repository.findFirstByCode(code);
    }

    @Override
    public List<SavingsGoalCategoryEntity> getSavingsGoalCategoryList() {
        return repository.getAllByRecordStatus(RecordStatusConstant.ACTIVE);
    }

    @Override
    public long countSavingsGoalCategory() {
        return repository.countAllByRecordStatus(RecordStatusConstant.ACTIVE);
    }

    @Override
    public Optional<SavingsGoalCategoryEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public SavingsGoalCategoryEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. SavingsGoalCategoryEntity with Id: "+aLong));
    }

    @Override
    public SavingsGoalCategoryEntity saveRecord(SavingsGoalCategoryEntity record) {
        return repository.save(record);
    }
}
