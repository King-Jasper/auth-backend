package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsPlanEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsPlanTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsPlanRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.retry.annotation.Retryable;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
@Named
public class SavingsPlanEntityDaoImpl implements SavingsPlanEntityDao {

    private SavingsPlanRepository repository;
    private AppSequenceEntityDao appSequenceEntityDao;
    public SavingsPlanEntityDaoImpl(SavingsPlanRepository repository, AppSequenceEntityDao appSequenceEntityDao) {
        this.repository = repository;
        this.appSequenceEntityDao = appSequenceEntityDao;
    }

    @Override
    public long countSavingPlans() {
        return repository.count();
    }

    @Override
    public List<SavingsPlanEntity> getSavingsPlans() {
        return repository.getAllByRecordStatus(RecordStatusConstant.ACTIVE);
    }

    @Override
    public SavingsPlanEntity getPlanByType(SavingsPlanTypeConstant planTypeConstant) {
        return repository.getFirstByRecordStatusAndPlanName(RecordStatusConstant.ACTIVE, planTypeConstant);
    }

    @Override
    public Optional<SavingsPlanEntity> findPlanByPlanId(String planId) {
        return repository.findFirstByPlanId(planId);
    }

    @Retryable(maxAttempts = 4)
    @Override
    public String generatePlanId() {
        return String.format("%s%06d%s", RandomStringUtils.randomNumeric(1),
                appSequenceEntityDao.getNextSequenceId(SequenceType.SAVINGS_PLAN_SEQ),
                RandomStringUtils.randomNumeric(1));
    }

    @Override
    public Optional<SavingsPlanEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public SavingsPlanEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. SavingsPlanEntity with Id: "+aLong));
    }

    @Override
    public SavingsPlanEntity saveRecord(SavingsPlanEntity record) {
        return repository.save(record);
    }
}
