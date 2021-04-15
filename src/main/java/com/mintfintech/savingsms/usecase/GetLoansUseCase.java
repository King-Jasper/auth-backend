package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.usecase.data.request.LoanSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import com.mintfintech.savingsms.usecase.models.LoanTransactionModel;

import java.util.List;

public interface GetLoansUseCase {

    PagedDataResponse<LoanModel> getPagedLoans(LoanSearchRequest searchRequest, int page, int size);

    LoanModel toLoanModel(LoanRequestEntity loanRequestEntity);

    List<LoanTransactionModel> getLoanTransactions(String loanId);

}
