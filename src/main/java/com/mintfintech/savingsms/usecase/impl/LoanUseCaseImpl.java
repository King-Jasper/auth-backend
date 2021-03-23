package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.CustomerLoanProfileEntityDao;
import com.mintfintech.savingsms.domain.dao.EmployeeInformationEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanTransactionEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import com.mintfintech.savingsms.domain.entities.EmployeeInformationEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.LoanTransactionEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.LoanUseCase;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoanUseCaseImpl implements LoanUseCase {

    private final LoanRequestEntityDao loanRequestEntityDao;
    private final LoanTransactionEntityDao loanTransactionEntityDao;
    private final ApplicationProperty applicationProperty;
    private final GetLoansUseCase getLoansUseCase;
    private final CustomerLoanProfileUseCase customerLoanProfileUseCase;
    private final EmployeeInformationEntityDao employeeInformationEntityDao;
    private final CustomerLoanProfileEntityDao customerLoanProfileEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final AppUserEntityDao appUserEntityDao;

    @Override
    @Transactional
    public LoanModel approveLoanRequest(AuthenticatedUser authenticatedUser, String loanId) {

        LoanRequestEntity loanRequestEntity = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        loanRequestEntity.setApprovalStatus(LoanApprovalStatusConstant.APPROVED);
        loanRequestEntity.setApprovedDate(LocalDateTime.now());
        loanRequestEntity.setApproveByName(authenticatedUser.getUsername());
        loanRequestEntity.setApproveByUserId(authenticatedUser.getUserId());
        loanRequestEntity.setRepaymentDueDate(LocalDateTime.now().plusDays(applicationProperty.getPayDayLoanMaxTenor()));

        loanRequestEntityDao.saveRecord(loanRequestEntity);

        //call core banking

        LoanTransactionEntity entity = LoanTransactionEntity.builder()
                .loanRequest(loanRequestEntity)
                .autoDebit(false)
                .externalReference("")
                .responseCode("")
                .responseMessage("")
                .transactionAmount(loanRequestEntity.getLoanAmount())
                .transactionReference(loanRequestEntityDao.generateLoanTransactionRef())
                .transactionStatus(TransactionStatusConstant.SUCCESSFUL)
                .transactionType(TransactionTypeConstant.CREDIT)
                .build();

        loanTransactionEntityDao.saveRecord(entity);

        return getLoansUseCase.toLoanModel(loanRequestEntity);
    }

    @Override
    public LoanModel rejectLoanRequest(AuthenticatedUser authenticatedUser, String loanId, String reason) {
        LoanRequestEntity loanRequestEntity = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        loanRequestEntity.setApprovalStatus(LoanApprovalStatusConstant.REJECTED);
        loanRequestEntity.setApproveByName(authenticatedUser.getUsername());
        loanRequestEntity.setApproveByUserId(authenticatedUser.getUserId());

        loanRequestEntityDao.saveRecord(loanRequestEntity);

        return getLoansUseCase.toLoanModel(loanRequestEntity);
    }

    @Override
    public BigDecimal getPendingRepaymentAmount(String loanId) {

        LoanRequestEntity loanRequestEntity = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        return loanRequestEntity.getRepaymentAmount().subtract(loanRequestEntity.getAmountPaid());
    }

    @Override
    public LoanModel loanRequest(AuthenticatedUser currentUser, double amount, String loanType) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

        MintBankAccountEntity mintAccount = mintBankAccountEntityDao.findByAccountId(currentUser.getAccountId())
                .orElseThrow(() -> new BadRequestException("No Bank Account for this user"));

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(appUser)
                .orElseThrow(() -> new BadRequestException("No Loan Profile exist for this user"));

        if (customerLoanProfileEntity.isBlacklisted()) {
            throw new BadRequestException("This user is blacklisted");
        }

        LoanRequestEntity loanRequestEntity = null;

        LoanTypeConstant loanTypeConstant = LoanTypeConstant.valueOf(loanType);

        if (loanTypeConstant.equals(LoanTypeConstant.PAYDAY)) {
            EmployeeInformationEntity employeeInformationEntity = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());

            if (!employeeInformationEntity.isVerified()) {
                throw new BadRequestException("This user employee information has not be verified");
            }

            BigDecimal maxLoanAmount = customerLoanProfileUseCase.getPayDayLoanMaxAmount(currentUser);
            BigDecimal loanAmount = BigDecimal.valueOf(amount);

            if (loanAmount.compareTo(maxLoanAmount) > 0) {
                throw new BadRequestException("Loan amount is higher than the maximum allowed for this user");
            }

            if (loanRequestEntityDao.countActiveLoan(appUser) > 0) {
                throw new BadRequestException("User has active loan");
            }

            loanRequestEntity = LoanRequestEntity.builder()
                    .bankAccount(mintAccount)
                    .loanId(loanRequestEntityDao.generateLoanId())
                    .interestRate(applicationProperty.getPayDayLoanInterestRate())
                    .loanAmount(loanAmount)
                    .repaymentAmount(loanAmount.add(loanAmount.multiply(BigDecimal.valueOf(applicationProperty.getPayDayLoanInterestRate() / 100.0))))
                    .requestedBy(appUser)
                    .loanType(LoanTypeConstant.PAYDAY)
                    .build();

            loanRequestEntity = loanRequestEntityDao.saveRecord(loanRequestEntity);
        }

        return buildPayDayLoanModel(loanRequestEntity);
    }

    private LoanModel buildPayDayLoanModel(LoanRequestEntity loanRequestEntity) {
        LoanModel model = new LoanModel();
        if (loanRequestEntity != null) {
            model.setLoanId(loanRequestEntity.getLoanId());
            model.setAmountPaid(loanRequestEntity.getAmountPaid().toPlainString());
            model.setLoanAmount(loanRequestEntity.getLoanAmount().toPlainString());
            model.setApprovalStatus(loanRequestEntity.getApprovalStatus().name());
            model.setInterestRate(loanRequestEntity.getInterestRate());
            model.setRepaymentAmount(loanRequestEntity.getRepaymentAmount().toPlainString());
            model.setRepaymentStatus(loanRequestEntity.getRepaymentStatus().name());
            model.setLoanType(loanRequestEntity.getLoanType().name());
        }
        return model;
    }
}
