package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.HNILoanCustomerEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.models.reports.HNICustomerSearchDTO;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 26 Sep, 2023
 */
public interface HNILoanCustomerEntityDao extends CrudDao<HNILoanCustomerEntity, Long>{
     Optional<HNILoanCustomerEntity> findRecord(MintAccountEntity mintAccount);
     Page<HNILoanCustomerEntity> getRecords(HNICustomerSearchDTO searchDTO, int page, int size);
}
