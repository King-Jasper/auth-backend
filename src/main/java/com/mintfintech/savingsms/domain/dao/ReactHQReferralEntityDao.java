package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.ReactHQReferralEntity;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 03 Mar, 2022
 */
public interface ReactHQReferralEntityDao extends CrudDao<ReactHQReferralEntity, Long>{
    Optional<ReactHQReferralEntity> findCustomerForDebit(String accountNumber);
    long countCustomerSupported();
    List<ReactHQReferralEntity> getCustomerForFundSupport(int size);
}
