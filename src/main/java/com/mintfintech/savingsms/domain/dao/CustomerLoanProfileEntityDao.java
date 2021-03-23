package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;

import java.util.List;
import java.util.Optional;

public interface CustomerLoanProfileEntityDao extends CrudDao<CustomerLoanProfileEntity, Long> {

    Optional<CustomerLoanProfileEntity> findCustomerProfileByAppUser(AppUserEntity appUserEntity);

    List<CustomerLoanProfileEntity> getCustomerWithUnverifiedEmployeeInformation();

    List<CustomerLoanProfileEntity> getCustomerWithVerifiedEmployeeInformation();
}
