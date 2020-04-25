package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;


import com.mintfintech.savingsms.domain.dao.SavingsPlanTenorEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanTenorEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsDurationTypeConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsPlanTenorRepository;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 10 Feb, 2020
 */
@Named
public class SavingsPlanTenorEntityDaoImpl implements SavingsPlanTenorEntityDao {
    private SavingsPlanTenorRepository repository;
    public SavingsPlanTenorEntityDaoImpl(SavingsPlanTenorRepository repository) {
        this.repository = repository;
    }

    @Override
    public long countSavingsPlanTenor() {
        return repository.count();
    }

    @Override
    public List<SavingsPlanTenorEntity> getTenorList() {
        return repository.getAllByRecordStatus(RecordStatusConstant.ACTIVE);
    }

    @Override
    public SavingsPlanTenorEntity getSavingPlanTenor(SavingsPlanEntity planEntity, int duration, SavingsDurationTypeConstant durationTypeConstant) {
        return repository.findFirstBySavingsPlanAndDurationTypeAndDurationAndRecordStatus(planEntity, durationTypeConstant, duration, RecordStatusConstant.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Not found. SavingsPlanTenorEntity with duration: "+duration));
    }

    @Override
    public SavingsPlanTenorEntity getLeastDurationOnSavingsPlan(SavingsPlanEntity planEntity) {
        return repository.getFirstBySavingsPlanOrderByDurationAsc(planEntity);
    }

    @Override
    public List<SavingsPlanTenorEntity> getTenorListByPlan(SavingsPlanEntity planEntity) {
        return repository.getAllByRecordStatusAndSavingsPlan(RecordStatusConstant.ACTIVE, planEntity);
    }

    @Override
    public Optional<SavingsPlanTenorEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public SavingsPlanTenorEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. SavingsPlanTenorEntity with id: "+aLong));
    }

    @Override
    public SavingsPlanTenorEntity saveRecord(SavingsPlanTenorEntity record) {
        return repository.save(record);
    }
}
