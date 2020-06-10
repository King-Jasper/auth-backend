package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.PagedResponse;
import com.mintfintech.savingsms.domain.models.SavingsSearchDTO;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsGoalRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Retryable;

import javax.inject.Named;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        return repository.getCurrentAccountGoals(accountEntity, RecordStatusConstant.ACTIVE);
    }

    @Override
    public Optional<SavingsGoalEntity> findFirstSavingsByType(MintAccountEntity accountEntity, SavingsGoalTypeConstant savingsGoalType) {
        return repository.findFirstByMintAccountAndSavingsGoalTypeAndRecordStatus(accountEntity, savingsGoalType, RecordStatusConstant.ACTIVE);
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
        return repository.findFirstByMintAccountAndGoalIdAndRecordStatus(accountEntity, goalId, RecordStatusConstant.ACTIVE);
    }

    @Override
    public List<SavingsGoalEntity> getSavingGoalWithAutoSaveTime(LocalDateTime autoSaveTime) {
        String currentHourTime = autoSaveTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH"));
       // int currentHour = String.format("%d-%02d-%02d %02d", autoSaveTime.getYear(), autoSaveTime.getMonthValue(), autoSaveTime) autoSaveTime.getHour();
        return repository.getSavingsGoalWithMatchingSavingHour(SavingsGoalStatusConstant.ACTIVE, currentHourTime);
    }

    @Override
    public PagedResponse<SavingsGoalEntity> getPagedSavingsGoalsWithMaturityDateWithinPeriod(LocalDateTime fromTime, LocalDateTime toTime, int pageIndex, int recordSize) {
        Pageable pageable = PageRequest.of(pageIndex, recordSize);
        Page<SavingsGoalEntity> goalEntityPage = repository.getSavingsGoalWithMaturityPeriod(SavingsGoalStatusConstant.ACTIVE, fromTime, toTime, pageable);
        return new PagedResponse<>(goalEntityPage.getTotalElements() ,goalEntityPage.getTotalPages(), goalEntityPage.getContent());
    }

    @Override
    public Page<SavingsGoalEntity> searchSavingsGoal(SavingsSearchDTO searchDTO, int pageIndex, int recordSize) {
        Pageable pageable = PageRequest.of(pageIndex, recordSize, Sort.by("dateCreated").descending());
        Specification<SavingsGoalEntity> specification = withActiveStatus();
        if(searchDTO.getToDate() != null && searchDTO.getFromDate() != null) {
            specification = specification.and(withDateRange(searchDTO.getFromDate(), searchDTO.getToDate()));
        }
        if(searchDTO.getAccount() != null) {
            specification = specification.and(withMintAccount(searchDTO.getAccount()));
        }
        if(!StringUtils.isEmpty(searchDTO.getGoalId())) {
            specification = specification.and(withGoalId(searchDTO.getGoalId()));
        }
        if(searchDTO.getGoalStatus() != null) {
            specification = specification.and(withGoalStatus(searchDTO.getGoalStatus()));
        }
        if(searchDTO.getSavingsPlan() != null) {
            specification = specification.and(withPlan(searchDTO.getSavingsPlan()));
        }
        if(searchDTO.getAutoSaveStatus() != null) {
            boolean autoSave = searchDTO.getAutoSaveStatus() == SavingsSearchDTO.AutoSaveStatus.ENABLED;
            specification = specification.and(withAutoSaveStatus(autoSave));
        }
        return repository.findAll(specification, pageable);
    }

    private static Specification<SavingsGoalEntity> withStatus() {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                .and(criteriaBuilder.equal(root.get("goalStatus"), SavingsGoalStatusConstant.ACTIVE),
                        criteriaBuilder.equal(root.get("creationSource"), SavingsGoalCreationSourceConstant.CUSTOMER),
                        criteriaBuilder.equal(root.get("autoSave"), true),
                        criteriaBuilder.isNotNull(root.get("nextAutoSaveDate"))
                        ));
    }

    @Override
    public long countUserCreatedSavingsGoalsOnPlan(MintAccountEntity mintAccountEntity, SavingsPlanEntity planEntity) {
        return repository.countActiveCustomerCreatedGoalsOnAccountAndPlan(RecordStatusConstant.ACTIVE, mintAccountEntity, planEntity, SavingsGoalTypeConstant.CUSTOMER_SAVINGS);
    }

    @Override
    public long countUserCreatedAccountSavingsGoals(MintAccountEntity mintAccountEntity) {
        return repository.countActiveCustomerCreatedGoalsOnAccount(RecordStatusConstant.ACTIVE, mintAccountEntity, SavingsGoalTypeConstant.CUSTOMER_SAVINGS);
    }

    @Override
    public Optional<SavingsGoalEntity> findGoalByNameAndPlanAndAccount(String name, SavingsPlanEntity planEntity, MintAccountEntity accountEntity) {
        return repository.findFirstByMintAccountAndSavingsPlanAndGoalStatusAndRecordStatusAndNameIgnoreCase(accountEntity, planEntity,
                SavingsGoalStatusConstant.ACTIVE, RecordStatusConstant.ACTIVE, name);
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


    private static Specification<SavingsGoalEntity> withActiveStatus() {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("recordStatus"), RecordStatusConstant.ACTIVE),
                criteriaBuilder.equal(root.get("creationSource"), SavingsGoalCreationSourceConstant.CUSTOMER)));
    }

    private static Specification<SavingsGoalEntity> withGoalStatus(SavingsGoalStatusConstant goalStatus) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("goalStatus"), goalStatus));
    }

    private static Specification<SavingsGoalEntity> withDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return ((fundTransferRoot, criteriaQuery, criteriaBuilder) -> criteriaBuilder.between(fundTransferRoot.get("dateCreated"), startDate, endDate));
    }

    private static Specification<SavingsGoalEntity> withMintAccount(MintAccountEntity account) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("mintAccount"), account.getId()));
    }

    private static Specification<SavingsGoalEntity> withGoalId(String goalId) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("goalId"), goalId));
    }

    private static Specification<SavingsGoalEntity> withPlan(SavingsPlanEntity savingsPlan) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("savingsPlan"), savingsPlan.getId()));
    }

    private static Specification<SavingsGoalEntity> withAutoSaveStatus(boolean autoSaveStatus) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("autoSave"), autoSaveStatus));
    }
}
