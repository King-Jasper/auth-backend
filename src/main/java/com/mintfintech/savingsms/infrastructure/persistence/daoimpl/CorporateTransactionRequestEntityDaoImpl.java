package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.CorporateTransactionRequestEntityDao;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.infrastructure.persistence.repository.CorporateTransactionRequestRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.StaleObjectStateException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.LockTimeoutException;
import javax.persistence.OptimisticLockException;
import java.util.Optional;

@Named
public class CorporateTransactionRequestEntityDaoImpl extends CrudDaoImpl<CorporateTransactionRequestEntity, Long> implements CorporateTransactionRequestEntityDao {

    private final CorporateTransactionRequestRepository repository;
    private final AppSequenceEntityDao appSequenceEntityDao;
    private final EntityManager entityManager;
    public CorporateTransactionRequestEntityDaoImpl(CorporateTransactionRequestRepository repository, AppSequenceEntityDao appSequenceEntityDao, EntityManager entityManager) {
        super(repository);
        this.appSequenceEntityDao = appSequenceEntityDao;
        this.repository = repository;
        this.entityManager = entityManager;
    }


    @Override
    public String generateRequestId() {

        int retries = 0;
        boolean success = false;
        String id = "CS"+ RandomStringUtils.random(8);
        while(!success && retries < 5) {
            try {
                id = String.format("CS%07d%s",appSequenceEntityDao.getNextSequenceId(SequenceType.INVESTMENT_SEQ), RandomStringUtils.randomNumeric(1));
                success = true;
            }catch (StaleObjectStateException | ObjectOptimisticLockingFailureException | OptimisticLockException | LockTimeoutException ex){
                retries++;
                success = false;
            }
        }
        if(retries >= 5) {
            id = "CS"+RandomStringUtils.random(8);
        }
        return id;
    }

    @Override
    public Optional<CorporateTransactionRequestEntity> findByRequestId(String requestId) {
        return repository.findTopByRequestIdAndRecordStatus(requestId, RecordStatusConstant.ACTIVE);
    }


}
