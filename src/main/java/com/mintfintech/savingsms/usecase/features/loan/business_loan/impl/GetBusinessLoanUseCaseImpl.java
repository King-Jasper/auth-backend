package com.mintfintech.savingsms.usecase.features.loan.business_loan.impl;

import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.models.LoanSearchDTO;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.BusinessLoanResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.features.loan.business_loan.GetBusinessLoanUseCase;
import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;

import javax.inject.Named;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Created by jnwanya on
 * Sat, 26 Feb, 2022
 */
@Named
@AllArgsConstructor
public class GetBusinessLoanUseCaseImpl implements GetBusinessLoanUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final LoanRequestEntityDao loanRequestEntityDao;

    @Override
    public PagedDataResponse<BusinessLoanResponse> getBusinessLoanDetails(AuthenticatedUser currentUser, int page, int size) {
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());
        LoanSearchDTO searchDTO = LoanSearchDTO.builder()
                .account(mintAccount)
                .loanType(LoanTypeConstant.BUSINESS)
                .build();
        Page<LoanRequestEntity> entityPage = loanRequestEntityDao.searchLoans(searchDTO, page, size);
        return new PagedDataResponse<>(entityPage.getTotalElements(), entityPage.getTotalPages(),
                entityPage.get().map(this::fromEntityToResponse).collect(Collectors.toList()));
    }

    @Override
    public BusinessLoanResponse fromEntityToResponse(LoanRequestEntity loanRequest) {
        if(!Hibernate.isInitialized(loanRequest)) {
            loanRequest = loanRequestEntityDao.getRecordById(loanRequest.getId());
        }
        BusinessLoanResponse response = BusinessLoanResponse.builder()
                .loanAmount(loanRequest.getLoanAmount())
                .repaymentAmount(loanRequest.getRepaymentAmount())
                .amountPaid(loanRequest.getAmountPaid())
                .dateApplied(loanRequest.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .status(loanRequest.getApprovalStatus().name())
                .durationInMonths(loanRequest.getDurationInMonths())
                .loanId(loanRequest.getLoanId())
                .build();
        if(loanRequest.getApprovalStatus() == ApprovalStatusConstant.APPROVED) {
            response.setDueDate(loanRequest.getRepaymentDueDate().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        return response;
    }
}
