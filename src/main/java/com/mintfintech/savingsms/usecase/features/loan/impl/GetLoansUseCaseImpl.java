package com.mintfintech.savingsms.usecase.features.loan.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanReviewStageConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.models.LoanSearchDTO;
import com.mintfintech.savingsms.usecase.features.loan.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.features.loan.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.data.request.LoanSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import com.mintfintech.savingsms.usecase.models.LoanTransactionModel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetLoansUseCaseImpl implements GetLoansUseCase {

    private final LoanRequestEntityDao loanRequestEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final CustomerLoanProfileUseCase customerLoanProfileUseCase;
    private final CustomerLoanProfileEntityDao customerLoanProfileEntityDao;
    private final LoanTransactionEntityDao loanTransactionEntityDao;

    @Override
    public PagedDataResponse<LoanModel> getPagedLoans(LoanSearchRequest searchRequest, int page, int size) {

        Optional<MintAccountEntity> mintAccount = mintAccountEntityDao.findAccountByAccountId(searchRequest.getAccountId());

        LoanSearchDTO searchDTO = LoanSearchDTO.builder()
                .fromDate(searchRequest.getFromDate() != null ? searchRequest.getFromDate().atStartOfDay() : null)
                .toDate(searchRequest.getToDate() != null ? searchRequest.getToDate().atTime(23, 59) : null)
                .repaymentStatus(!searchRequest.getRepaymentStatus().equals("ALL") ? LoanRepaymentStatusConstant.valueOf(searchRequest.getRepaymentStatus()) : null)
                .approvalStatus(!searchRequest.getApprovalStatus().equals("ALL") ? ApprovalStatusConstant.valueOf(searchRequest.getApprovalStatus()) : null)
                .account(mintAccount.orElse(null))
                .loanType(searchRequest.getLoanType() != null ? LoanTypeConstant.valueOf(searchRequest.getLoanType()) : null)
                .customerName(searchRequest.getCustomerName())
                .customerPhone(searchRequest.getCustomerPhone())
                .build();

        Page<LoanRequestEntity> goalEntityPage = loanRequestEntityDao.searchLoans(searchDTO, page, size);

        return new PagedDataResponse<>(goalEntityPage.getTotalElements(), goalEntityPage.getTotalPages(),
                goalEntityPage.get().map(this::toLoanModel)
                        .collect(Collectors.toList()));
    }

    @Override
    public LoanModel toLoanModel(LoanRequestEntity loanRequestEntity) {
        LoanModel loanModel = new LoanModel();

        ApprovalStatusConstant approvalStatus = loanRequestEntity.getApprovalStatus();
        LoanRepaymentStatusConstant repaymentStatus = loanRequestEntity.getRepaymentStatus();

        Optional<CustomerLoanProfileEntity> customerLoanProfile = customerLoanProfileEntityDao.findCustomerProfileByAppUser(loanRequestEntity.getRequestedBy());

        List<LoanTransactionEntity> debitTransactions = loanTransactionEntityDao.getDebitLoanTransactions(loanRequestEntity);

        loanModel.setLoanId(loanRequestEntity.getLoanId());
        loanModel.setLoanType(loanRequestEntity.getLoanType().name());
        loanModel.setLoanAmount(loanRequestEntity.getLoanAmount());
        loanModel.setAmountPaid(loanRequestEntity.getAmountPaid());
        loanModel.setApprovalStatus(approvalStatus.name());
        loanModel.setInterestRate(loanRequestEntity.getInterestRate());
        loanModel.setRepaymentAmount(loanRequestEntity.getRepaymentAmount());
        loanModel.setRepaymentStatus(repaymentStatus.name());
        loanModel.setRepaymentDueDate(loanRequestEntity.getRepaymentDueDate() != null ? loanRequestEntity.getRepaymentDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        loanModel.setCreatedDate(loanRequestEntity.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE));
        loanModel.setApprovedDate(loanRequestEntity.getApprovedDate() != null ? loanRequestEntity.getApprovedDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        loanModel.setLastPaymentDate(debitTransactions.isEmpty() ? null : debitTransactions.get(0).getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE));
        loanModel.setOwner(customerLoanProfile.map(customerLoanProfileUseCase::toLoanCustomerProfileModel).orElse(null));
        loanModel.setRejectionReason(StringUtils.defaultString(loanRequestEntity.getRejectionReason()));
        if(loanRequestEntity.getApprovalStatus() == ApprovalStatusConstant.DECLINED || loanRequestEntity.getApprovalStatus() == ApprovalStatusConstant.REJECTED) {
            loanModel.setDateRejected(loanRequestEntity.getDateRejected() != null ?
                    loanRequestEntity.getDateRejected().format(DateTimeFormatter.ISO_DATE_TIME) :
                    loanRequestEntity.getDateModified().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        loanModel.setReviewStage(loanRequestEntity.getReviewStage() == null ? LoanReviewStageConstant.FIRST_REVIEW.name(): loanRequestEntity.getReviewStage().name());
        String loanStatus = "";
        if(approvalStatus == ApprovalStatusConstant.PENDING) {
            loanStatus = "PENDING";
        }else if(approvalStatus == ApprovalStatusConstant.DECLINED || approvalStatus == ApprovalStatusConstant.CANCELLED) {
            loanStatus = "DECLINED";
        }else {
            // IT IS APPROVED
            if(repaymentStatus == LoanRepaymentStatusConstant.PAID || repaymentStatus == LoanRepaymentStatusConstant.COMPLETED) {
                loanStatus = "COMPLETED";
            }else if(repaymentStatus == LoanRepaymentStatusConstant.PENDING || repaymentStatus == LoanRepaymentStatusConstant.PARTIALLY_PAID) {
                if(loanRequestEntity.getRepaymentDueDate().isBefore(LocalDateTime.now())) {
                    loanStatus = "OVERDUE";
                }else {
                    loanStatus = "ACTIVE";
                }
            }else if(repaymentStatus == LoanRepaymentStatusConstant.CANCELLED) {
                loanStatus = "DECLINED";
            }
        }
        loanModel.setClientLoanStatus(loanStatus);
        return loanModel;
    }

    @Override
    public List<LoanTransactionModel> getLoanTransactions(String loanId) {
        LoanRequestEntity loanRequestEntity = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        List<LoanTransactionEntity> transactions = loanTransactionEntityDao.getLoanTransactions(loanRequestEntity);

        List<LoanTransactionModel> transactionModels = new ArrayList<>();

        for (LoanTransactionEntity entity : transactions) {

            LoanTransactionModel model = new LoanTransactionModel();
            model.setAmount(entity.getTransactionAmount());
            model.setReference(entity.getTransactionReference());
            model.setExternalReference(entity.getExternalReference());
            model.setResponseCode(entity.getResponseCode());
            model.setStatus(entity.getStatus().name());
            model.setResponseMessage(entity.getResponseMessage());
            model.setType(entity.getTransactionType().name());
            model.setPaymentDate(entity.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE));

            transactionModels.add(model);
        }

        return transactionModels;
    }
}
