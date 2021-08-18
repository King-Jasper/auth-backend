package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AccumulatedInterestEntityDao;
import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.entities.AccumulatedInterestEntity;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.AccumulatedInterestRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Named;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by jnwanya on
 * Fri, 29 May, 2020
 */
@Named
public class AccumulatedInterestEntityDaoImpl extends CrudDaoImpl<AccumulatedInterestEntity, Long> implements AccumulatedInterestEntityDao {

    private final AccumulatedInterestRepository repository;
    private final AppSequenceEntityDao appSequenceEntityDao;
    public AccumulatedInterestEntityDaoImpl(AccumulatedInterestRepository repository, AppSequenceEntityDao appSequenceEntityDao) {
        super(repository);
        this.repository = repository;
        this.appSequenceEntityDao = appSequenceEntityDao;
    }

    @Value("${accumulated-interest.reference.prefix:MAI}")
    private String referencePrefix;

    @Override
    public String generatedReference() {
        return String.format("%s%08d%s", referencePrefix, appSequenceEntityDao.getNextSequenceId(SequenceType.ACCUMULATED_INTEREST_REFERENCE_SEQ), RandomStringUtils.randomNumeric(1));
    }

    @Override
    public List<AccumulatedInterestEntity> getFailedInterestRecord(LocalDateTime fromDate, LocalDateTime toDate) {
        return repository.getAllByTransactionStatusAndDateCreatedBetween(TransactionStatusConstant.FAILED, fromDate, toDate);
    }
}
