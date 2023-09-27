package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.HNILoanCustomerEntityDao;
import com.mintfintech.savingsms.domain.entities.HNILoanCustomerEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.AccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.models.InvestmentSearchDTO;
import com.mintfintech.savingsms.domain.models.reports.HNICustomerSearchDTO;
import com.mintfintech.savingsms.infrastructure.persistence.repository.HNILoanCustomerRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.inject.Named;
import javax.persistence.criteria.*;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 26 Sep, 2023
 */
@Named
public class HNILoanCustomerEntityDaoImpl extends CrudDaoImpl<HNILoanCustomerEntity, Long> implements HNILoanCustomerEntityDao {

    private final HNILoanCustomerRepository repository;

    public HNILoanCustomerEntityDaoImpl(HNILoanCustomerRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Optional<HNILoanCustomerEntity> findRecord(MintAccountEntity mintAccount) {
        return repository.findTopByCustomer(mintAccount);
    }

    @Override
    public Page<HNILoanCustomerEntity> getRecords(HNICustomerSearchDTO searchDTO, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateModified").descending());
        Specification<HNILoanCustomerEntity> specification = (root, query, criteriaBuilder) -> buildSearchQuery(searchDTO, root, query, criteriaBuilder);
        return repository.findAll(specification, pageable);
    }

    private Predicate buildSearchQuery(HNICustomerSearchDTO searchDTO, Root<HNILoanCustomerEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate whereClause = cb.equal(root.get("recordStatus"), RecordStatusConstant.ACTIVE);

        if (searchDTO.getMintAccount() != null) {
            whereClause = cb.and(whereClause, cb.equal(root.get("customer"), searchDTO.getMintAccount().getId()));
        }

        if (searchDTO.getRepaymentPlanType() != null) {
            whereClause = cb.and(whereClause, cb.equal(root.get("repaymentPlanType"), searchDTO.getRepaymentPlanType()));
        }
       if(StringUtils.isNotEmpty(searchDTO.getCustomerName())) {
           Join<HNILoanCustomerEntity, MintAccountEntity> accountJoin = root.join("customer");
           if (StringUtils.isNotEmpty(searchDTO.getCustomerName())) {
               whereClause = cb.and(whereClause, cb.like(cb.lower(accountJoin.get("name")), "%" + searchDTO.getCustomerName().toLowerCase() + "%"));
           }
       }
       return whereClause;
    }
}
