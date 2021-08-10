package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.CustomerLoanProfileEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import com.mintfintech.savingsms.domain.entities.EmployeeInformationEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanReviewStageConstant;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.models.CustomerLoanProfileSearchDTO;
import com.mintfintech.savingsms.infrastructure.persistence.repository.CustomerLoanProfileRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.inject.Named;
import javax.persistence.criteria.Join;
import java.time.LocalDateTime;
import java.util.Optional;

@Named
public class CustomerLoanProfileEntityDaoImpl implements CustomerLoanProfileEntityDao {

    private final CustomerLoanProfileRepository repository;

    public CustomerLoanProfileEntityDaoImpl(CustomerLoanProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CustomerLoanProfileEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public CustomerLoanProfileEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. CustomerLoanProfileEntity with Id :"+aLong));
    }

    @Override
    public CustomerLoanProfileEntity saveRecord(CustomerLoanProfileEntity record) {
        return repository.save(record);
    }

    @Override
    public Optional<CustomerLoanProfileEntity> findCustomerProfileByAppUser(AppUserEntity appUserEntity) {
        return repository.findByAppUser(appUserEntity);
    }

    @Override
    public Page<CustomerLoanProfileEntity> searchVerifiedCustomerProfile(CustomerLoanProfileSearchDTO searchDTO, int pageIndex, int recordSize) {
        Pageable pageable = PageRequest.of(pageIndex, recordSize, Sort.by("dateCreated").descending());
        Specification<CustomerLoanProfileEntity> specification = withActiveStatus();

        if (searchDTO.getToDate() != null && searchDTO.getFromDate() != null) {
            specification = specification.and(withDateRange(searchDTO.getFromDate(), searchDTO.getToDate()));
        }

        if (searchDTO.getVerificationStatus() != null) {
            specification = specification.and(withVerificationStatus(searchDTO.getVerificationStatus()));
        }

        if(searchDTO.getReviewStage() != null) {
            specification = specification.and(withReviewStage(searchDTO.getReviewStage()));
        }

        if(StringUtils.isNotEmpty(searchDTO.getCustomerName())) {
            String name = searchDTO.getCustomerName();
            Specification<CustomerLoanProfileEntity> temp = (root, query, criteriaBuilder) -> {
                Join<CustomerLoanProfileEntity, AppUserEntity> appUserJoin = root.join("appUser");
                return criteriaBuilder.like(criteriaBuilder.lower(appUserJoin.get("name")), "%"+name+"%");
            };
            specification = specification.and(temp);
        }
        if(StringUtils.isNotEmpty(searchDTO.getCustomerPhone())) {
            String phoneNumber = searchDTO.getCustomerPhone();
            Specification<CustomerLoanProfileEntity> temp = (root, query, criteriaBuilder) -> {
                Join<CustomerLoanProfileEntity, AppUserEntity> appUserJoin = root.join("appUser");
                return criteriaBuilder.equal(appUserJoin.get("phoneNumber"), phoneNumber);
            };
            specification = specification.and(temp);
        }
        return repository.findAll(specification, pageable);
    }

    private static Specification<CustomerLoanProfileEntity> withVerificationStatus(ApprovalStatusConstant verificationStatus) {
       return (root, criteriaQuery, criteriaBuilder) -> {
            Join<CustomerLoanProfileEntity, EmployeeInformationEntity> employeeInformationJoin = root.join("employeeInformation");
            return criteriaBuilder.equal(employeeInformationJoin.get("verificationStatus"), verificationStatus);
        };
    }

    private static Specification<CustomerLoanProfileEntity> withReviewStage(LoanReviewStageConstant reviewStageConstant) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            Join<CustomerLoanProfileEntity, EmployeeInformationEntity> employeeInformationJoin = root.join("employeeInformation");
            return criteriaBuilder.equal(employeeInformationJoin.get("reviewStage"), reviewStageConstant);
        };
    }

    private static Specification<CustomerLoanProfileEntity> withActiveStatus() {
        return ((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("recordStatus"), RecordStatusConstant.ACTIVE)));
    }

    private static Specification<CustomerLoanProfileEntity> withDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return ((fundTransferRoot, criteriaQuery, criteriaBuilder) -> criteriaBuilder.between(fundTransferRoot.get("dateCreated"), startDate, endDate));
    }

}
