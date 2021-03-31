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
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.LoanUseCase;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import com.mintfintech.savingsms.usecase.models.LoanTransactionModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanUseCaseImpl implements LoanUseCase {

    private final LoanRequestEntityDao loanRequestEntityDao;
    private final LoanTransactionEntityDao loanTransactionEntityDao;
    private final ApplicationProperty applicationProperty;
    private final GetLoansUseCase getLoansUseCase;
    private final EmployeeInformationEntityDao employeeInformationEntityDao;
    private final CustomerLoanProfileEntityDao customerLoanProfileEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final AppUserEntityDao appUserEntityDao;

    @Transactional
    @Override
    public LoanModel approveLoanRequest(AuthenticatedUser authenticatedUser, String loanId, String reason, boolean approved) {
        LoanRequestEntity loanRequestEntity = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        if (approved) {
            loanRequestEntity.setApprovalStatus(ApprovalStatusConstant.APPROVED);
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
        } else {
            loanRequestEntity.setApprovalStatus(ApprovalStatusConstant.REJECTED);
            loanRequestEntity.setApproveByName(authenticatedUser.getUsername());
            loanRequestEntity.setApproveByUserId(authenticatedUser.getUserId());

            loanRequestEntityDao.saveRecord(loanRequestEntity);
        }

        return getLoansUseCase.toLoanModel(loanRequestEntity);
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

        LoanTypeConstant loanTypeConstant = LoanTypeConstant.valueOf(loanType);

        BigDecimal maxLoanAmount = getLoanMaxAmount(customerLoanProfileEntity, loanTypeConstant);

        BigDecimal loanAmount = BigDecimal.valueOf(amount);

        if (loanAmount.compareTo(maxLoanAmount) > 0) {
            throw new BadRequestException("Loan amount is higher than the maximum allowed for this user");
        }

        if (loanRequestEntityDao.countActiveLoan(appUser) > 0) {
            throw new BadRequestException("User has active loan");
        }

        LoanRequestEntity loanRequestEntity = null;

        if (loanTypeConstant.equals(LoanTypeConstant.PAYDAY)) {
            loanRequestEntity = payDayLoanRequest(loanAmount, mintAccount, appUser);
        }

        return getLoansUseCase.toLoanModel(loanRequestEntity);

    }

    @Override
    @Transactional
    public LoanModel repayment(AuthenticatedUser currentUser, double amount, String loanId) {
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

        MintBankAccountEntity mintAccount = mintBankAccountEntityDao.findByAccountId(currentUser.getAccountId())
                .orElseThrow(() -> new BadRequestException("No Bank Account for this user"));

        LoanRequestEntity loanRequestEntity = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        BigDecimal repaymentAmount = BigDecimal.valueOf(amount);
        BigDecimal amountPending = loanRequestEntity.getRepaymentAmount().subtract(loanRequestEntity.getAmountPaid());

        if (repaymentAmount.compareTo(amountPending) > 0) {
            repaymentAmount = amountPending;
        }

        // Call Core banking

        BigDecimal amountPaid = loanRequestEntity.getAmountPaid().add(repaymentAmount);
        loanRequestEntity.setAmountPaid(amountPaid);
        loanRequestEntity.setRepaymentStatus(amountPaid.compareTo(loanRequestEntity.getRepaymentAmount()) < 0 ? LoanRepaymentStatusConstant.PARTIALLY_PAID : LoanRepaymentStatusConstant.PAID);

        loanRequestEntity = loanRequestEntityDao.saveRecord(loanRequestEntity);

        LoanTransactionEntity entity = LoanTransactionEntity.builder()
                .loanRequest(loanRequestEntity)
                .autoDebit(false)
                .externalReference("")
                .responseCode("")
                .responseMessage("")
                .transactionAmount(repaymentAmount)
                .transactionReference(loanRequestEntityDao.generateLoanTransactionRef())
                .transactionStatus(TransactionStatusConstant.SUCCESSFUL)
                .transactionType(TransactionTypeConstant.DEBIT)
                .build();

        entity = loanTransactionEntityDao.saveRecord(entity);

        updateCustomerRating(appUser);

        return getLoansUseCase.toLoanModel(loanRequestEntity);
    }

    @Override
    public LoanModel getLoanTransactions(String loanId) {
        LoanRequestEntity loanRequestEntity = loanRequestEntityDao.findByLoanId(loanId)
                .orElseThrow(() -> new BadRequestException("Loan request for this loanId " + loanId + " does not exist"));

        LoanModel loanModel = getLoansUseCase.toLoanModel(loanRequestEntity);

        List<LoanTransactionEntity> transactions = loanTransactionEntityDao.getLoanTransactions(loanRequestEntity);

        List<LoanTransactionModel> transactionModels = new ArrayList<>();

        for (LoanTransactionEntity entity : transactions) {

            LoanTransactionModel model = new LoanTransactionModel();
            model.setAmount(entity.getTransactionAmount().toPlainString());
            model.setReference(entity.getTransactionReference());
            model.setExternalReference(entity.getExternalReference());
            model.setResponseCode(entity.getResponseCode());
            model.setStatus(entity.getTransactionStatus().name());
            model.setResponseMessage(entity.getResponseMessage());
            model.setType(entity.getTransactionType().name());
            model.setPaymentDate(entity.getDateCreated());

            transactionModels.add(model);
        }

        loanModel.setTransactions(transactionModels);
        return loanModel;
    }

    @Override
    public void updateCustomerRating(AppUserEntity currentUser) {

        CustomerLoanProfileEntity customerLoanProfileEntity = customerLoanProfileEntityDao.findCustomerProfileByAppUser(currentUser)
                .orElseThrow(() -> new BadRequestException("No Loan Profile exist for this user"));


        double totalLoanCount = loanRequestEntityDao.countTotalLoans(currentUser);
        double totalRepaymentFailed = loanRequestEntityDao.countTotalLoansPastRepaymentDueDate(currentUser);

        double rating = ((totalLoanCount - totalRepaymentFailed)/totalLoanCount) * 5.0;

        customerLoanProfileEntity.setRating(rating);

        customerLoanProfileEntityDao.saveRecord(customerLoanProfileEntity);
    }

    private LoanRequestEntity payDayLoanRequest(
            BigDecimal loanAmount,
            MintBankAccountEntity mintAccount,
            AppUserEntity appUser
    ) {

        LoanRequestEntity loanRequestEntity = LoanRequestEntity.builder()
                .bankAccount(mintAccount)
                .loanId(loanRequestEntityDao.generateLoanId())
                .interestRate(applicationProperty.getPayDayLoanInterestRate())
                .loanAmount(loanAmount)
                .repaymentAmount(loanAmount.add(loanAmount.multiply(BigDecimal.valueOf(applicationProperty.getPayDayLoanInterestRate() / 100.0))))
                .requestedBy(appUser)
                .loanType(LoanTypeConstant.PAYDAY)
                .build();

        return loanRequestEntityDao.saveRecord(loanRequestEntity);
    }

    private BigDecimal getLoanMaxAmount(CustomerLoanProfileEntity customerLoanProfileEntity, LoanTypeConstant loanTypeConstant) {

        BigDecimal maxAmount = BigDecimal.ZERO;

        if (loanTypeConstant.equals(LoanTypeConstant.PAYDAY)) {

            if (customerLoanProfileEntity.getEmployeeInformation() == null) {
                throw new BadRequestException("An Employee Information does not exist for this user.");
            }

            EmployeeInformationEntity employeeInformationEntity = employeeInformationEntityDao.getRecordById(customerLoanProfileEntity.getEmployeeInformation().getId());

            maxAmount = employeeInformationEntity.getMonthlyIncome().multiply(BigDecimal.valueOf(applicationProperty.getPayDayMaxLoanPercentAmount() / 100.0));

        }

        return maxAmount;
    }

}
