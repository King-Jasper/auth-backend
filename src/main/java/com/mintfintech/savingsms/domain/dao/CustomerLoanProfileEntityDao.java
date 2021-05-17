package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import com.mintfintech.savingsms.domain.models.CustomerLoanProfileSearchDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface CustomerLoanProfileEntityDao extends CrudDao<CustomerLoanProfileEntity, Long> {

    Optional<CustomerLoanProfileEntity> findCustomerProfileByAppUser(AppUserEntity appUserEntity);

    Page<CustomerLoanProfileEntity> searchVerifiedCustomerProfile(CustomerLoanProfileSearchDTO searchDTO, int pageIndex, int recordSize);
}
