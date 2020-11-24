package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.SavingsFundingRequestEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsFundingRequestEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsFundingRequestRepository;
import javax.inject.Named;
import java.util.Optional;
/**
 * Created by jnwanya on
 * Thu, 22 Oct, 2020
 */
@Named
public class SavingsFundingRequestEntityDaoImpl extends CrudDaoImpl<SavingsFundingRequestEntity, Long> implements SavingsFundingRequestEntityDao {

    private final SavingsFundingRequestRepository repository;
    public SavingsFundingRequestEntityDaoImpl(SavingsFundingRequestRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Optional<SavingsFundingRequestEntity> findByPaymentReference(String reference) {
        return repository.findTopByPaymentReference(reference);
    }
}
