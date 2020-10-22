package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.SavingsFundingRequestEntity;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 22 Oct, 2020
 */
public interface SavingsFundingRequestEntityDao extends CrudDao<SavingsFundingRequestEntity, Long>{
    Optional<SavingsFundingRequestEntity> findByPaymentReference(String reference);
}
