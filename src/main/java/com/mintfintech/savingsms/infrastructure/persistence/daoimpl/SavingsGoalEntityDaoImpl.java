package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.PagedResponse;
import com.mintfintech.savingsms.domain.models.SavingsSearchDTO;
import com.mintfintech.savingsms.domain.models.reports.AmountModel;
import com.mintfintech.savingsms.domain.models.reports.SavingsMaturityStat;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsGoalRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.StaleObjectStateException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.LockTimeoutException;
import javax.persistence.OptimisticLockException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
@Slf4j
@Named
public class SavingsGoalEntityDaoImpl extends CrudDaoImpl<SavingsGoalEntity, Long> implements SavingsGoalEntityDao {

    private final SavingsGoalRepository repository;
    private final AppSequenceEntityDao appSequenceEntityDao;
    private final EntityManager entityManager;
    public SavingsGoalEntityDaoImpl(SavingsGoalRepository repository, AppSequenceEntityDao appSequenceEntityDao, EntityManager entityManager) {
        super(repository);
        this.repository = repository;
        this.appSequenceEntityDao = appSequenceEntityDao;
        this.entityManager = entityManager;
    }
    
    @Override
    public String generateSavingGoalId() {
        int retries = 0;
        boolean success = false;
        String goalId = RandomStringUtils.random(8);
        while(!success && retries < 5) {
            try {
               goalId = String.format("%s%06d%s",
                       RandomStringUtils.randomNumeric(1),
                       appSequenceEntityDao.getNextSequenceIdTemp(SequenceType.SAVINGS_GOAL_SEQ),
                       RandomStringUtils.randomNumeric(1));
               success = true;
            }catch (StaleObjectStateException | ObjectOptimisticLockingFailureException | OptimisticLockException | LockTimeoutException ex){
                log.info("savings-exception caught - {},  goalId - {}, retries - {}", ex.getClass().getSimpleName(), goalId, retries);
                retries++;
                success = false;
            }
            if(retries > 0 && success) {
                 log.info("Successful retrieval of unique goal Id - {}", goalId);
            }
        }
        if(retries >= 5) {
            goalId = RandomStringUtils.random(8);
        }
        return goalId;
    }

    @Override
    public List<SavingsGoalEntity> getAccountSavingGoals(MintAccountEntity accountEntity) {
        return repository.getCurrentAccountGoals(accountEntity, RecordStatusConstant.ACTIVE);
    }

    @Override
    public Optional<SavingsGoalEntity> findFirstSavingsByType(MintAccountEntity accountEntity, SavingsGoalTypeConstant savingsGoalType) {
        return repository.findFirstByMintAccountAndSavingsGoalTypeAndRecordStatusOrderByDateCreatedDesc(accountEntity, savingsGoalType, RecordStatusConstant.ACTIVE);
    }

    @Override
    public Optional<SavingsGoalEntity> findFirstSavingsByTypeIgnoreStatus(MintAccountEntity accountEntity, SavingsGoalTypeConstant savingsGoalType) {
        return repository.findFirstByMintAccountAndSavingsGoalTypeOrderByDateCreatedDesc(accountEntity, savingsGoalType);
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
    public Optional<SavingsGoalEntity> findSavingGoalByGoalId(String goalId) {
        return repository.findFirstByGoalIdAndRecordStatus(goalId, RecordStatusConstant.ACTIVE);
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
        Specification<SavingsGoalEntity> specification = (root, query, criteriaBuilder) -> buildSearchQuery(searchDTO, root, query, criteriaBuilder);
        return repository.findAll(specification, pageable);
    }

    @Override
    public BigDecimal sumSearchedSavingsGoal(SavingsSearchDTO savingsSearchDTO) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AmountModel> query = cb.createQuery(AmountModel.class);
        Root<SavingsGoalEntity> root = query.from(SavingsGoalEntity.class);

        Predicate whereClause = buildSearchQuery(savingsSearchDTO, root, query, cb);

        query.select(cb.construct(AmountModel.class, cb.sum(root.get("savingsBalance"))));

        query.where(whereClause);
        TypedQuery<AmountModel> typedQuery = entityManager.createQuery(query);
        BigDecimal amount = typedQuery.getSingleResult().getAmount();
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private Predicate buildSearchQuery(SavingsSearchDTO searchDTO, Root<SavingsGoalEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

        Predicate whereClause = cb.equal(root.get("recordStatus"), RecordStatusConstant.ACTIVE);

        if(searchDTO.getToDate() != null && searchDTO.getFromDate() != null) {
            whereClause = cb.and(whereClause, cb.between(root.get("dateCreated"), searchDTO.getFromDate(), searchDTO.getToDate()));
        }
        if(searchDTO.getAccount() != null) {
            whereClause = cb.and(whereClause, cb.equal(root.get("mintAccount"), searchDTO.getAccount().getId()));
        }
        if(!StringUtils.isEmpty(searchDTO.getGoalId())) {
            whereClause = cb.and(whereClause, cb.equal(root.get("goalId"), searchDTO.getGoalId()));
        }
        if(searchDTO.getGoalStatus() != null) {
            whereClause = cb.and(whereClause, cb.equal(root.get("goalStatus"), searchDTO.getGoalStatus()));
        }
        if(searchDTO.getGoalType() != null) {
            whereClause = cb.and(whereClause, cb.equal(root.get("savingsGoalType"), searchDTO.getGoalType()));
        }
        if(searchDTO.getAutoSaveStatus() != null) {
            boolean autoSave = searchDTO.getAutoSaveStatus() == SavingsSearchDTO.AutoSaveStatus.ENABLED;
            whereClause = cb.and(whereClause, cb.equal(root.get("autoSave"), autoSave));
        }
        if(StringUtils.isNotEmpty(searchDTO.getGoalName())) {
            String name = searchDTO.getGoalName().toLowerCase();
            whereClause = cb.and(whereClause, cb.like(cb.lower(root.get("name")), name+"%"));
        }

        if(StringUtils.isNotEmpty(searchDTO.getCustomerName())) {
            String name = searchDTO.getCustomerName().toLowerCase();
            Join<SavingsGoalEntity, AppUserEntity> owner = root.join("creator");
            whereClause = cb.and(whereClause, cb.like(cb.lower(owner.get("name")), "%"+name+"%"));
        }
        return whereClause;
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
    public List<SavingsMaturityStat> savingsMaturityStatisticsList(LocalDateTime startDate, LocalDateTime endDate) {
         return repository.getSavingsMaturityStatistics(startDate, endDate);
    }

    @Transactional
    @Override
    public void deleteSavings(SavingsGoalEntity savingsGoalEntity) {
        repository.delete(savingsGoalEntity);
    }

    @Override
    public List<SavingsGoalEntity> getAllSavingsByType(MintAccountEntity accountEntity, SavingsGoalTypeConstant goalType) {
        return repository.getAllSavingsByType(accountEntity, goalType);
    }

    @Override
    public Optional<SavingsGoalEntity> findGoalByNameAndPlanAndAccountAndType(String name, SavingsPlanEntity savingsPlan, MintAccountEntity mintAccount, SavingsGoalTypeConstant goalTypeConstant) {
        return repository.findFirstByMintAccountAndSavingsPlanAndGoalStatusAndRecordStatusAndNameAndSavingsGoalTypeIgnoreCase(mintAccount, savingsPlan,
                SavingsGoalStatusConstant.ACTIVE, RecordStatusConstant.ACTIVE, name, goalTypeConstant);
    }
}
