package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.CorporateTransactionRequestEntityDao;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.domain.models.reports.CorporateTransactionSearchDTO;
import com.mintfintech.savingsms.infrastructure.persistence.repository.CorporateTransactionRequestRepository;
import org.apache.commons.lang3.RandomStringUtils;
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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Optional;

@Named
public class CorporateTransactionRequestEntityDaoImpl extends CrudDaoImpl<CorporateTransactionRequestEntity, Long> implements CorporateTransactionRequestEntityDao {

    private final CorporateTransactionRequestRepository repository;
    private final AppSequenceEntityDao appSequenceEntityDao;
    private final EntityManager entityManager;
    public CorporateTransactionRequestEntityDaoImpl(CorporateTransactionRequestRepository repository, AppSequenceEntityDao appSequenceEntityDao, EntityManager entityManager) {
        super(repository);
        this.appSequenceEntityDao = appSequenceEntityDao;
        this.repository = repository;
        this.entityManager = entityManager;
    }


    @Override
    public String generateRequestId() {

        int retries = 0;
        boolean success = false;
        String id = "CS"+ RandomStringUtils.random(8);
        while(!success && retries < 5) {
            try {
                id = String.format("CS%07d%s",appSequenceEntityDao.getNextSequenceId(SequenceType.INVESTMENT_SEQ), RandomStringUtils.randomNumeric(1));
                success = true;
            }catch (StaleObjectStateException | ObjectOptimisticLockingFailureException | OptimisticLockException | LockTimeoutException ex){
                retries++;
                success = false;
            }
        }
        if(retries >= 5) {
            id = "CS"+RandomStringUtils.random(8);
        }
        return id;
    }

    @Override
    public Optional<CorporateTransactionRequestEntity> findByRequestId(String requestId) {
        return repository.findTopByRequestId(requestId);
    }

    @Override
    public Page<CorporateTransactionRequestEntity> searchTransaction(CorporateTransactionSearchDTO searchDTO, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreated").descending());
        Specification<CorporateTransactionRequestEntity> specification = (root, query, criteriaBuilder) -> buildSearchQuery(searchDTO, root, query, criteriaBuilder);
        return repository.findAll(specification,pageable);
    }

    private Predicate buildSearchQuery(CorporateTransactionSearchDTO searchDTO, Root<CorporateTransactionRequestEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate whereClause = cb.equal(root.get("recordStatus"), RecordStatusConstant.ACTIVE);
        whereClause = cb.and(whereClause, cb.equal(root.get("corporate"), searchDTO.getCorporate().getId()));

        if (searchDTO.getToDate() != null && searchDTO.getFromDate() != null) {
            whereClause = cb.and(whereClause, cb.between(root.get("dateCreated"), searchDTO.getFromDate(), searchDTO.getToDate()));
        }
        if (searchDTO.getApprovalStatus() != null){
            whereClause = cb.and(whereClause, cb.equal(root.get("approvalStatus"), searchDTO.getApprovalStatus()));
        }
        if(searchDTO.getTransactionType() != null){
            whereClause = cb.and(whereClause, cb.equal(root.get("transactionType"), searchDTO.getTransactionType()));
        }
        return whereClause;
    }

}
