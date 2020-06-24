package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AccumulatedInterestEntityDao;
import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.entities.AccumulatedInterestEntity;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.infrastructure.persistence.repository.AccumulatedInterestRepository;
import org.apache.commons.lang3.RandomStringUtils;

import javax.inject.Named;

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

    @Override
    public String generatedReference() {
        return String.format("MAI%08d%s", appSequenceEntityDao.getNextSequenceId(SequenceType.ACCUMULATED_INTEREST_REFERENCE_SEQ), RandomStringUtils.randomNumeric(1));
    }
}