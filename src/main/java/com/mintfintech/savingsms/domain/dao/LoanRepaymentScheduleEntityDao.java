package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.LoanRepaymentScheduleEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;

import java.util.List;

/**
 * Created by jnwanya on
 * Wed, 27 Sep, 2023
 */
public interface LoanRepaymentScheduleEntityDao extends CrudDao<LoanRepaymentScheduleEntity, Long>{
    List<LoanRepaymentScheduleEntity> getRecords(LoanRequestEntity loanRequest);
}
