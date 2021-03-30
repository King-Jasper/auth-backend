package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant;
import com.mintfintech.savingsms.usecase.LoanRepaymentUseCase;
import com.mintfintech.savingsms.usecase.LoanUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanRepaymentUseCaseImpl implements LoanRepaymentUseCase {

    private static final int NUMBER_OF_DAYS_UPFRONT = 2;
    private final LoanRequestEntityDao loanRequestEntityDao;
    private final LoanUseCase loanUseCase;
    private final AppUserEntityDao appUserEntityDao;

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

            loanUseCase.updateCustomerRating(loanOwner);

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


}
