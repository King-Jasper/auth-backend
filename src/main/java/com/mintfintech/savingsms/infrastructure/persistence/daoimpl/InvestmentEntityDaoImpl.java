package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.domain.models.InvestmentSearchDTO;
import com.mintfintech.savingsms.domain.models.reports.InvestmentStat;
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
import javax.persistence.LockTimeoutException;
import javax.persistence.OptimisticLockException;
import javax.persistence.criteria.Join;
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

    public InvestmentEntityDaoImpl(InvestmentRepository repository, AppSequenceEntityDao appSequenceEntityDao) {
        super(repository);
        this.repository = repository;
        this.appSequenceEntityDao = appSequenceEntityDao;
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

        Specification<InvestmentEntity> specification = withActiveStatus();

        if (searchDTO.getStartToDate() != null && searchDTO.getStartFromDate() != null) {
            specification = specification.and(withStartDateRange(searchDTO.getStartFromDate(), searchDTO.getStartToDate()));
        }

        if (searchDTO.getMatureFromDate() != null && searchDTO.getMatureToDate() != null) {
            specification = specification.and(withMaturityDateRange(searchDTO.getMatureFromDate(), searchDTO.getMatureToDate()));
        }

        if (searchDTO.getAccount() != null) {
            specification = specification.and(withMintAccount(searchDTO.getAccount()));
        }
        if(searchDTO.getAccount() != null) {
            if (searchDTO.getInvestmentStatus() != null) {
                if(searchDTO.getInvestmentStatus() == InvestmentStatusConstant.COMPLETED) {
                    specification = specification.and(
                            withInvestmentStatus(InvestmentStatusConstant.COMPLETED)
                                    .or(withInvestmentStatus(InvestmentStatusConstant.LIQUIDATED))
                    );
                }else {
                    specification = specification.and(withInvestmentStatus(searchDTO.getInvestmentStatus()));
                }
            }
        }else {
            if (searchDTO.getInvestmentStatus() != null) {
                if(searchDTO.isCompletedRecords()) {
                    specification = specification.and(
                            withInvestmentStatus(InvestmentStatusConstant.COMPLETED)
                                    .or(withInvestmentStatus(InvestmentStatusConstant.LIQUIDATED)));
                }else {
                    specification = specification.and(withInvestmentStatus(searchDTO.getInvestmentStatus()));
                }
            }
        }
        /*
        if (searchDTO.getInvestmentStatus() != null) {
            if(searchDTO.getInvestmentStatus() == InvestmentStatusConstant.COMPLETED) {
                if(searchDTO.getAccount() == null) {
                    specification = specification.and(withInvestmentStatus(searchDTO.getInvestmentStatus()));
                }else {
                    specification = specification.and(
                            withInvestmentStatus(InvestmentStatusConstant.COMPLETED)
                                    .or(withInvestmentStatus(InvestmentStatusConstant.LIQUIDATED))
                    );
                }
            }else {
                specification = specification.and(withInvestmentStatus(searchDTO.getInvestmentStatus()));
            }
        }*/

        if (searchDTO.getDuration() != 0) {
            specification = specification.and(withDuration(searchDTO.getDuration()));
        }

        if (StringUtils.isNotEmpty(searchDTO.getCustomerName())) {
            Specification<InvestmentEntity> temp = (root, query, criteriaBuilder) -> {
                Join<InvestmentEntity, MintAccountEntity> accountJoin = root.join("owner");
                return criteriaBuilder.like(criteriaBuilder.lower(accountJoin.get("name")), "%"+searchDTO.getCustomerName().toLowerCase()+"%");
            };
            specification = specification.and(temp);
        }

        return repository.findAll(specification, pageable);
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
