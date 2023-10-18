package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.LoanRepaymentScheduleEntityDao;
import com.mintfintech.savingsms.domain.entities.LoanRepaymentScheduleEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.LoanRepaymentScheduleRepository;

import javax.inject.Named;
import java.util.List;

/**
 * Created by jnwanya on
 * Wed, 27 Sep, 2023
 */
@Named
public class LoanRepaymentScheduleEntityDaoImpl extends CrudDaoImpl<LoanRepaymentScheduleEntity, Long> implements LoanRepaymentScheduleEntityDao {
    private final LoanRepaymentScheduleRepository repository;
    public LoanRepaymentScheduleEntityDaoImpl(LoanRepaymentScheduleRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public List<LoanRepaymentScheduleEntity> getRecords(LoanRequestEntity loanRequest) {
        return repository.getAllByRecordStatusAndLoanRequestOrderByRepaymentDueDateAsc(RecordStatusConstant.ACTIVE, loanRequest);
    }
}
