package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.CustomerLoanProfileEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.CustomerLoanProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.inject.Named;
import java.util.List;
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
        return repository.findByUser(appUserEntity);
    }

    @Override
    public List<CustomerLoanProfileEntity> getCustomerWithUnverifiedEmployeeInformation() {
        return repository.findCustomerEmployeeInformation(false);
    }

    @Override
    public List<CustomerLoanProfileEntity> getCustomerWithVerifiedEmployeeInformation() {
        return repository.findCustomerEmployeeInformation(true);
    }
}
