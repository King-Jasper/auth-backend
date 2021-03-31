package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.CustomerLoanProfileEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.models.LoanSearchDTO;
import com.mintfintech.savingsms.usecase.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.data.request.LoanSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetLoansUseCaseImpl implements GetLoansUseCase {

    private final LoanRequestEntityDao loanRequestEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final CustomerLoanProfileUseCase customerLoanProfileUseCase;
    private final CustomerLoanProfileEntityDao customerLoanProfileEntityDao;

    @Override
    public PagedDataResponse<LoanModel> getPagedLoans(LoanSearchRequest searchRequest, int page, int size) {

        Optional<MintBankAccountEntity> mintAccount = mintBankAccountEntityDao.findByAccountId(searchRequest.getAccountId());

        LoanSearchDTO searchDTO = LoanSearchDTO.builder()
                .fromDate(searchRequest.getFromDate() != null ? searchRequest.getFromDate().atStartOfDay() : null)
                .toDate(searchRequest.getToDate() != null ? searchRequest.getToDate().atTime(23, 59) : null)
                .status(searchRequest.getLoanStatus() != null ? LoanRepaymentStatusConstant.valueOf(searchRequest.getLoanStatus()) : null)
                .approvalStatus(searchRequest.getApprovalStatus() != null ? ApprovalStatusConstant.valueOf(searchRequest.getApprovalStatus()) : null)
                .account(mintAccount.orElse(null))
                .loanType(searchRequest.getLoanType() != null ? LoanTypeConstant.valueOf(searchRequest.getLoanType()) : null)
                .build();

        Page<LoanRequestEntity> goalEntityPage = loanRequestEntityDao.searchLoans(searchDTO, page, size);

        return new PagedDataResponse<>(goalEntityPage.getTotalElements(), goalEntityPage.getTotalPages(),
                goalEntityPage.get().map(this::toLoanModel)
                        .collect(Collectors.toList()));
    }

    @Override
    public LoanModel toLoanModel(LoanRequestEntity loanRequestEntity) {
        LoanModel loanModel = new LoanModel();

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(loanRequestEntity.getRequestedBy())
                .orElseThrow(() -> new BadRequestException("No profile exist"));


        loanModel.setLoanId(loanRequestEntity.getLoanId());
        loanModel.setLoanType(loanRequestEntity.getLoanType().name());
        loanModel.setLoanAmount(loanRequestEntity.getLoanAmount().toPlainString());
        loanModel.setAmountPaid(loanRequestEntity.getAmountPaid().toPlainString());
        loanModel.setApprovalStatus(loanRequestEntity.getApprovalStatus().name());
        loanModel.setInterestRate(loanRequestEntity.getInterestRate());
        loanModel.setRepaymentAmount(loanRequestEntity.getRepaymentAmount().toPlainString());
        loanModel.setRepaymentStatus(loanRequestEntity.getRepaymentStatus().name());
        loanModel.setRepaymentDueDate(loanRequestEntity.getRepaymentDueDate());
        loanModel.setOwner(customerLoanProfileUseCase.toLoanCustomerProfileModel(customerLoanProfileEntity));

        return loanModel;
    }
}
