package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.infrastructure.persistence.repository.InvestmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.StaleObjectStateException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import javax.inject.Named;
import javax.persistence.LockTimeoutException;
import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 17 May, 2021
 */
@Slf4j
@Named
public class InvestmentEntityDaoImpl extends CrudDaoImpl<InvestmentEntity, Long> implements InvestmentEntityDao {

    private final InvestmentRepository repository;
    private final AppSequenceEntityDao appSequenceEntityDao;

    public InvestmentEntityDaoImpl(InvestmentRepository repository, AppSequenceEntityDao appSequenceEntityDao) {
        super(repository);
        this.repository = repository;
        this.appSequenceEntityDao = appSequenceEntityDao;
    }

    @Override
    public String generateCode() {
        int retries = 0;
        boolean success = false;
        String id = RandomStringUtils.randomNumeric(8);
        while(!success && retries < 5) {
            try {
                id = String.format("%s%06d%s", RandomStringUtils.randomNumeric(1), appSequenceEntityDao.getNextSequenceIdTemp(SequenceType.INVESTMENT_SEQ), RandomStringUtils.randomNumeric(1));
                success = true;
            }catch (StaleObjectStateException | ObjectOptimisticLockingFailureException | OptimisticLockException | LockTimeoutException ex){
                log.info("exception caught - {},  id - {}, retries - {}", ex.getClass().getSimpleName(), id, retries);
                retries++;
                success = false;
            }
            if(retries > 0 && success) {
                log.info("Successful retrieval of unique Id - {}", id);
            }
        }
        if(retries >= 5) {
            id = RandomStringUtils.randomNumeric(8);
        }
        return id;
    }

    @Override
    public List<InvestmentEntity> getRecordsOnAccount(MintAccountEntity mintAccountEntity) {
        return repository.getAllByOwnerAndRecordStatus(mintAccountEntity, RecordStatusConstant.ACTIVE);
    }

    @Override
    public Optional<InvestmentEntity> findByCode(String code) {
        return repository.findTopByCodeIgnoreCase(code);
    }

    @Override
    public Page<InvestmentEntity> getRecordsForEligibleInterestApplication(int pageIndex, int recordSize) {
        Pageable pageable = PageRequest.of(pageIndex, recordSize);
        return repository.getEligibleInterestInvestment(pageable);
    }

    @Override
    public Page<InvestmentEntity> getRecordsWithMaturityDateWithinPeriod(LocalDateTime fromTime, LocalDateTime toTime, int pageIndex, int recordSize) {
        Pageable pageable = PageRequest.of(pageIndex, recordSize);
        return repository.getInvestmentWithMaturityPeriod(SavingsGoalStatusConstant.ACTIVE, fromTime, toTime, pageable);
    }
}
