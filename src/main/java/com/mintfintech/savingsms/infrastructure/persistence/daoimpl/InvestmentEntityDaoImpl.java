package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.AccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.domain.models.InvestmentSearchDTO;
import com.mintfintech.savingsms.domain.models.reports.AmountModel;
import com.mintfintech.savingsms.domain.models.reports.InvestmentStat;
import com.mintfintech.savingsms.domain.models.reports.SavingsMaturityStat;
import com.mintfintech.savingsms.infrastructure.persistence.repository.InvestmentRepository;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 17 May, 2021
 */
@Slf4j
@Named
public class InvestmentEntityDaoImpl extends CrudDaoImpl<InvestmentEntity, Long> implements InvestmentEntityDao {

    private final InvestmentRepository repository;
    private final AppSequenceEntityDao appSequenceEntityDao;
    private final EntityManager entityManager;
    public InvestmentEntityDaoImpl(InvestmentRepository repository, AppSequenceEntityDao appSequenceEntityDao, EntityManager entityManager) {
        super(repository);
        this.repository = repository;
        this.appSequenceEntityDao = appSequenceEntityDao;
        this.entityManager = entityManager;
    }

    @Override
    public String generateCode() {
        int retries = 0;
        boolean success = false;
        String id = RandomStringUtils.randomNumeric(8);
        while (!success && retries < 5) {
            try {
                id = String.format("%s%06d%s", RandomStringUtils.randomNumeric(1), appSequenceEntityDao.getNextSequenceIdTemp(SequenceType.INVESTMENT_SEQ), RandomStringUtils.randomNumeric(1));
                success = true;
            } catch (StaleObjectStateException | ObjectOptimisticLockingFailureException | OptimisticLockException | LockTimeoutException ex) {
                log.info("exception caught - {},  id - {}, retries - {}", ex.getClass().getSimpleName(), id, retries);
                retries++;
                success = false;
            }
            if (retries > 0 && success) {
                log.info("Successful retrieval of unique Id - {}", id);
            }
        }
        if (retries >= 5) {
            id = RandomStringUtils.randomNumeric(8);
        }
        return id;
    }

    @Override
    public List<InvestmentEntity> getRecordsOnAccount(MintAccountEntity mintAccountEntity) {
        return repository.getAllByOwnerAndRecordStatus(mintAccountEntity, RecordStatusConstant.ACTIVE);
    }

    @Override
    public Optional<InvestmentEntity> findByCode(String code) {
        return repository.findTopByCodeIgnoreCase(code);
    }

    @Override
    public Page<InvestmentEntity> searchInvestments(InvestmentSearchDTO searchDTO, int pageIndex, int recordSize) {
        Pageable pageable = PageRequest.of(pageIndex, recordSize, Sort.by("dateCreated").descending());
        Specification<InvestmentEntity> specification = (root, query, criteriaBuilder) -> buildSearchQuery(searchDTO, root, query, criteriaBuilder);
        return repository.findAll(specification, pageable);
    }

    @Override
    public List<SavingsMaturityStat> getInvestmentMaturityStatistics(LocalDateTime fromDate, LocalDateTime toDate) {
        return repository.getInvestmentMaturityStatistics(fromDate, toDate);
    }

    @Override
    public boolean getByReferralCodeAndAppUser(String referralCode, AppUserEntity appUser) {
        return repository.existsByReferralCodeAndCreator(referralCode, appUser);
    }

    @Override
    public BigDecimal sumSearchedInvestments(InvestmentSearchDTO investmentSearchDTO) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AmountModel> query = cb.createQuery(AmountModel.class);
        Root<InvestmentEntity> root = query.from(InvestmentEntity.class);

        Predicate whereClause = buildSearchQuery(investmentSearchDTO, root, query, cb);

        query.select(cb.construct(AmountModel.class, cb.sum(root.get("amountInvested"))));

        query.where(whereClause);
        TypedQuery<AmountModel> typedQuery = entityManager.createQuery(query);
        BigDecimal amount = typedQuery.getSingleResult().getAmount();
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private Predicate buildSearchQuery(InvestmentSearchDTO searchDTO, Root<InvestmentEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate whereClause = cb.equal(root.get("recordStatus"), RecordStatusConstant.ACTIVE);

        if (searchDTO.getStartToDate() != null && searchDTO.getStartFromDate() != null) {
            whereClause = cb.and(whereClause, cb.between(root.get("dateCreated"), searchDTO.getStartFromDate(), searchDTO.getStartToDate()));
        }
        if (searchDTO.getMatureFromDate() != null && searchDTO.getMatureToDate() != null) {
            whereClause = cb.and(whereClause, cb.between(root.get("maturityDate"), searchDTO.getMatureFromDate(), searchDTO.getMatureToDate()));
        }
        if (searchDTO.getAccount() != null) {
            whereClause = cb.and(whereClause, cb.equal(root.get("owner"), searchDTO.getAccount().getId()));
        }

        if (searchDTO.getInvestmentStatus() != null) {
            if (searchDTO.getAccount() != null && searchDTO.getInvestmentStatus().equals(InvestmentStatusConstant.COMPLETED)) {
                whereClause = cb.and(whereClause, cb.or(cb.equal(root.get("investmentStatus"), InvestmentStatusConstant.LIQUIDATED),
                        cb.equal(root.get("investmentStatus"), InvestmentStatusConstant.COMPLETED)));
            } else {
                whereClause = cb.and(whereClause, cb.equal(root.get("investmentStatus"), searchDTO.getInvestmentStatus()));
            }
        } else if (searchDTO.isCompletedRecords()) {
            whereClause = cb.and(whereClause, cb.or(cb.equal(root.get("investmentStatus"), InvestmentStatusConstant.LIQUIDATED),
                    cb.equal(root.get("investmentStatus"), InvestmentStatusConstant.COMPLETED)));
        }
        if (searchDTO.getDuration() != 0) {
            whereClause = cb.and(whereClause, cb.equal(root.get("durationInMonths"), searchDTO.getDuration()));
        }

        Join<InvestmentEntity, MintAccountEntity> accountJoin = root.join("owner");
        if (StringUtils.isNotEmpty(searchDTO.getCustomerName())) {
            whereClause = cb.and(whereClause, cb.like(cb.lower(accountJoin.get("name")), "%" + searchDTO.getCustomerName().toLowerCase() + "%"));
        }

        if (!StringUtils.isEmpty(searchDTO.getAccountType())) {
            if ("INDIVIDUAL".equalsIgnoreCase(searchDTO.getAccountType())) {
                whereClause = cb.and(whereClause, cb.equal(accountJoin.get("accountType"), AccountTypeConstant.INDIVIDUAL));
            } else {
                whereClause = cb.and(whereClause, cb.notEqual(accountJoin.get("accountType"), AccountTypeConstant.INDIVIDUAL));
            }
        }
        return whereClause;
    }

    @Override
    public Page<InvestmentEntity> getRecordsForEligibleInterestApplication(int pageIndex, int recordSize) {
        Pageable pageable = PageRequest.of(pageIndex, recordSize);
        return repository.getEligibleInterestInvestment(pageable);
    }

    @Override
    public Page<InvestmentEntity> getRecordsWithMaturityDateWithinPeriod(LocalDateTime fromTime, LocalDateTime toTime, int pageIndex, int recordSize) {
        Pageable pageable = PageRequest.of(pageIndex, recordSize);
        return repository.getInvestmentWithMaturityPeriod(InvestmentStatusConstant.ACTIVE, fromTime, toTime, pageable);
    }

    @Override
    public List<InvestmentStat> getInvestmentStatOnAccount(MintAccountEntity mintAccountEntity) {
        return repository.getInvestmentStatistics(mintAccountEntity);
    }

    @Override
    public List<InvestmentStat> getStatsForCompletedInvestment(MintAccountEntity mintAccountEntity) {
        return repository.getStatisticsForCompletedInvestment(mintAccountEntity);
    }

    private static Specification<InvestmentEntity> withActiveStatus() {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("recordStatus"), RecordStatusConstant.ACTIVE)));
    }

    private static Specification<InvestmentEntity> withStartDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.between(root.get("dateCreated"), startDate, endDate));
    }

    private static Specification<InvestmentEntity> withMaturityDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.between(root.get("maturityDate"), startDate, endDate));
    }

    private static Specification<InvestmentEntity> withInvestmentStatus(InvestmentStatusConstant status) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("investmentStatus"), status));
    }

    private static Specification<InvestmentEntity> withDuration(int duration) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("durationInMonths"), duration));
    }

    private static Specification<InvestmentEntity> withMintAccount(MintAccountEntity account) {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("owner"), account.getId()));
    }

}
