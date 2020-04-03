package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.domain.models.PagedResponse;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsGoalRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Retryable;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
@Named
public class SavingsGoalEntityDaoImpl implements SavingsGoalEntityDao {

    private SavingsGoalRepository repository;
    private AppSequenceEntityDao appSequenceEntityDao;
    public SavingsGoalEntityDaoImpl(SavingsGoalRepository repository, AppSequenceEntityDao appSequenceEntityDao) {
        this.repository = repository;
        this.appSequenceEntityDao = appSequenceEntityDao;
    }

    @Retryable
    @Override
    public String generateSavingGoalId() {
        return String.format("%s%06d%s", RandomStringUtils.randomNumeric(1),
                appSequenceEntityDao.getNextSequenceId(SequenceType.SAVINGS_GOAL_SEQ),
                RandomStringUtils.randomNumeric(1));
    }

    @Override
    public List<SavingsGoalEntity> getAccountSavingGoals(MintAccountEntity accountEntity) {
        return repository.getAllByMintAccountAndRecordStatusOrderByDateCreatedDesc(accountEntity, RecordStatusConstant.ACTIVE);
    }

    @Override
    public long countEligibleInterestSavingsGoal() {
        return repository.countEligibleInterestSavingsGoal();
    }

    @Override
    public PagedResponse<SavingsGoalEntity> getPagedEligibleInterestSavingsGoal(int pageIndex, int recordSize) {
        Pageable pageable = PageRequest.of(pageIndex, recordSize);
        Page<SavingsGoalEntity> goalEntityPage = repository.getEligibleInterestSavingsGoal(pageable);
        return new PagedResponse<>(goalEntityPage.getTotalElements() ,goalEntityPage.getTotalPages(), goalEntityPage.getContent());
    }

    @Override
    public Optional<SavingsGoalEntity> findSavingGoalByAccountAndGoalId(MintAccountEntity accountEntity, String goalId) {
        return repository.findFirstByMintAccountAndGoalId(accountEntity, goalId);
    }

    @Override
    public long countAccountSavingsGoalOnPlan(MintAccountEntity mintAccountEntity, SavingsPlanEntity planEntity) {
        return repository.countAllByRecordStatusAndMintAccountAndSavingsPlan(RecordStatusConstant.ACTIVE, mintAccountEntity, planEntity);
    }

    @Override
    public Optional<SavingsGoalEntity> findGoalByNameAndPlanAndAccount(String name, SavingsPlanEntity planEntity, MintAccountEntity accountEntity) {
        return repository.findFirstByMintAccountAndSavingsPlanAndRecordStatusAndNameIgnoreCase(accountEntity, planEntity, RecordStatusConstant.ACTIVE, name);
    }

    @Override
    public Optional<SavingsGoalEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public SavingsGoalEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. SavingsGoalEntity with id: "+aLong));
    }

    @Override
    public SavingsGoalEntity saveRecord(SavingsGoalEntity record) {
        return repository.save(record);
    }
}
