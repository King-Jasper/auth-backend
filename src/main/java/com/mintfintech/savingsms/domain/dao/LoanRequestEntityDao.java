package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.models.LoanSearchDTO;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface LoanRequestEntityDao extends CrudDao<LoanRequestEntity, Long> {

    long countActiveLoan(AppUserEntity appUserEntity);

    String generateLoanId();

    String generateLoanTransactionRef();

    Page<LoanRequestEntity> searchLoans(LoanSearchDTO loanSearchDTO, int pageIndex, int recordSize);

    Optional<LoanRequestEntity> findByLoanId(String aLong);
}
