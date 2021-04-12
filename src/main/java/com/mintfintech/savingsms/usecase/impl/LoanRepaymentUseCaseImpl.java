package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanTransactionEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.LoanTransactionEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.LoanRepaymentUseCase;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanRepaymentUseCaseImpl implements LoanRepaymentUseCase {

    private static final int NUMBER_OF_DAYS_UPFRONT = 2;
    private final LoanRequestEntityDao loanRequestEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final CustomerLoanProfileUseCase customerLoanProfileUseCase;
    private final LoanTransactionEntityDao loanTransactionEntityDao;
    private final GetLoansUseCase getLoansUseCase;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;


    @Override
    public void dispatchEmailToCustomersWithPaymentDueInTwoDays() {
        List<LoanRequestEntity> paymentsDueInTwoDays = loanRequestEntityDao.getLoanRepaymentDueInDays(NUMBER_OF_DAYS_UPFRONT);
    }

    @Override
    public void dispatchEmailNotificationRepaymentOnDueDay() {
        List<LoanRequestEntity> repaymentDueToday = loanRequestEntityDao.getLoanRepaymentDueToday();


    }

    @Override
    public void checkDefaultedRepayment() {
        List<LoanRequestEntity> repaymentDueToday = loanRequestEntityDao.getLoanRepaymentDueToday();

        repaymentDueToday.forEach(loanRequestEntity -> {
            loanRequestEntity.setRepaymentStatus(LoanRepaymentStatusConstant.FAILED);
            loanRequestEntityDao.saveRecord(loanRequestEntity);

            AppUserEntity loanOwner = appUserEntityDao.getRecordById(loanRequestEntity.getRequestedBy().getId());

            customerLoanProfileUseCase.updateCustomerRating(loanOwner);

            //call core banking
        });
    }

    @Override
    public void processPaymentOfDueRepayment() {

        List<LoanRequestEntity> defaultedRepayments = loanRequestEntityDao.getDefaultedUnpaidLoanRepayment();

        for (LoanRequestEntity loanRequestEntity : defaultedRepayments) {
            //call core banking
        }

    }

    @Override
    @Transactional
    public LoanModel repayment(AuthenticatedUser currentUser, double amount, String loanId) {
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());

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

        customerLoanProfileUseCase.updateCustomerRating(appUser);

        return getLoansUseCase.toLoanModel(loanRequestEntity);
    }


}
