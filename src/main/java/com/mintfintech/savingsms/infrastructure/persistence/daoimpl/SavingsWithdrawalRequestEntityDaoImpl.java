package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsWithdrawalRequestEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsWithdrawalRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import com.mintfintech.savingsms.domain.models.SavingsSearchDTO;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsWithdrawalRequestRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.inject.Named;
import javax.persistence.criteria.*;
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
    public String generateTransactionReference() {
        return String.format("MSW%08d%s", appSequenceEntityDao.getNextSequenceId(SequenceType.SAVING_INTEREST_REFERENCE_SEQ), RandomStringUtils.randomNumeric(1));
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
//   implemented this
    @Override
    public Page<SavingsWithdrawalRequestEntity> getSavingsWithdrawalReport(SavingsSearchDTO searchDTO, int pageIndex, int recordSize) {
        Pageable pageable = PageRequest.of(pageIndex, recordSize, Sort.by("dateCreated").descending());
        Specification<SavingsWithdrawalRequestEntity> specification = (root, query, criteriaBuilder) -> buildSearchQuery(searchDTO, root, query, criteriaBuilder);
        return repository.findAll(specification, pageable);
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

    private Predicate buildSearchQuery(SavingsSearchDTO searchDTO, Root<SavingsWithdrawalRequestEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

        Predicate whereClause = cb.equal(root.get("recordStatus"), RecordStatusConstant.ACTIVE);

        if(searchDTO.getToDate() != null && searchDTO.getFromDate() != null) {
            whereClause = cb.and(whereClause, cb.between(root.get("dateCreated"), searchDTO.getFromDate(), searchDTO.getToDate()));
        }

        if(searchDTO.getWithdrawalStatus() != null) {
            whereClause = cb.and(whereClause, cb.equal(root.get("withdrawalRequestStatus"), searchDTO.getWithdrawalStatus()));
        }
        if(StringUtils.isNotEmpty(searchDTO.getCustomerName())) {
            String name = searchDTO.getCustomerName().toLowerCase();
            Join<SavingsGoalEntity, AppUserEntity> owner = root.join("creator");
            whereClause = cb.and(whereClause, cb.like(cb.lower(owner.get("name")), "%"+name+"%"));
        }
        return whereClause;
    }
}
