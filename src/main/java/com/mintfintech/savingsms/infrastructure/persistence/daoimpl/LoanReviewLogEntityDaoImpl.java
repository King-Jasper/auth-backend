package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.LoanReviewLogEntityDao;
import com.mintfintech.savingsms.domain.entities.LoanReviewLogEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanReviewLogType;
import com.mintfintech.savingsms.infrastructure.persistence.repository.LoanReviewLogRepository;

import javax.inject.Named;
import java.util.List;

/**
 * Created by jnwanya on
 * Mon, 09 Aug, 2021
 */
@Named
public class LoanReviewLogEntityDaoImpl extends CrudDaoImpl<LoanReviewLogEntity, Long> implements LoanReviewLogEntityDao {

    private final LoanReviewLogRepository repository;
    public LoanReviewLogEntityDaoImpl(LoanReviewLogRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public List<LoanReviewLogEntity> getRecordByReviewType(LoanReviewLogType reviewLogType) {
        return repository.getAllByReviewLogTypeOrderByDateCreatedDesc(reviewLogType);
    }
}
