package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.entities.AppSequenceEntity;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.infrastructure.persistence.repository.AppSequenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.LockTimeoutException;
import javax.persistence.PessimisticLockException;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.Random;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
@Slf4j
@Named
public class AppSequenceEntityDaoImpl implements AppSequenceEntityDao {


    private final AppSequenceRepository repository;
    public AppSequenceEntityDaoImpl(AppSequenceRepository repository) {
        this.repository = repository;
    }


    private EntityManager entityManager;
    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    //@Transactional
    @Override
    public Long getNextSequenceId(SequenceType sequenceType) {
        return nextId(sequenceType);
    }

    /**
     * Gets the next sequence number of a specific type.
     *
     * @return The next sequence number of a specific type.
     */
    //@Retryable(value = {StaleObjectStateException.class, ObjectOptimisticLockingFailureException.class, PessimisticLockException.class, LockTimeoutException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    // @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public Long nextId(SequenceType sequenceType){
        /*Random random = new Random();
        int delay = random.nextInt(10) * 500;
        System.out.println("delay - "+delay);
        */
        boolean success = false;
        int retries = 0;
        long id = 0;
        long versionValue = 0;
       // AppSequenceEntity appSequenceEntity = getSequenceRecord(sequenceType);
        while(!success && retries < 5) {
            try {
                /*AppSequenceEntity appSequenceEntity = repository.findFirstBySequenceType(sequenceType)
                        .orElseGet(() -> new AppSequenceEntity(sequenceType));
                */
                AppSequenceEntity appSequenceEntity = getSequenceRecord(sequenceType);
                id = appSequenceEntity.getValue();
                versionValue = appSequenceEntity.getVersion();
               // System.out.println(delay +" - version after refresh - "+versionValue);
                repository.saveAndFlush(appSequenceEntity);
                success = true;
            }catch (StaleObjectStateException | ObjectOptimisticLockingFailureException | LockTimeoutException ex){
                log.info("exception caught - {},  id - {}, retries - {} version - {}", ex.getClass().getSimpleName(), id, retries, versionValue);
                retries++;
                success = false;
                try {Thread.sleep(500);}catch (Exception ignored){};
               // log.info("delay {} - after exception - version {}", delay, versionValue);
               // appSequenceEntity = getSequenceRecord(sequenceType);
               // log.info("After exception version - {}", appSequenceEntity.getVersion());
            }
        }
        return id;
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public AppSequenceEntity getSequenceRecord(SequenceType sequenceType) {
        //entityManager.clear();
       // return repository.findSequenceRecord(sequenceType).orElseGet(() -> new AppSequenceEntity(sequenceType));

        TypedQuery<AppSequenceEntity> query = entityManager.createQuery(
                "SELECT a FROM AppSequenceEntity a WHERE a.sequenceType = :sequence" , AppSequenceEntity.class);
        AppSequenceEntity sequenceEntity = query.setParameter("sequence", sequenceType).getSingleResult();
        return sequenceEntity == null ? new AppSequenceEntity(sequenceType) : sequenceEntity;
    }
}
