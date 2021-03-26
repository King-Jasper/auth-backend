package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.entities.AppSequenceEntity;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.infrastructure.persistence.repository.AppSequenceRepository;
import org.hibernate.StaleObjectStateException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import javax.inject.Named;
import javax.persistence.LockTimeoutException;
import javax.persistence.PessimisticLockException;
import javax.transaction.Transactional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
@Named
public class AppSequenceEntityDaoImpl implements AppSequenceEntityDao {

    private final AppSequenceRepository repository;
    public AppSequenceEntityDaoImpl(AppSequenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Long getNextSequenceId(SequenceType sequenceType) {
        return nextId(sequenceType);
    }

    /**
     * Gets the next sequence number of a specific type.
     *
     * @return The next sequence number of a specific type.
     */
    @Retryable(value = {StaleObjectStateException.class, ObjectOptimisticLockingFailureException.class, PessimisticLockException.class, LockTimeoutException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    @Transactional
    public Long nextId(SequenceType sequenceType){
        AppSequenceEntity appSequenceEntity = repository.findFirstBySequenceType(sequenceType)
                .orElseGet(() -> new AppSequenceEntity(sequenceType));
        Long id = appSequenceEntity.getValue();
        repository.saveAndFlush(appSequenceEntity);
        return id;
    }
}
