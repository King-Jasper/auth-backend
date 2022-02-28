package com.mintfintech.savingsms.usecase.features.loan.business_loan.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.events.outgoing.LoanEmailEvent;
import com.mintfintech.savingsms.usecase.data.response.BusinessLoanResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.features.loan.business_loan.CreateBusinessLoanUseCase;
import com.mintfintech.savingsms.usecase.features.loan.business_loan.GetBusinessLoanUseCase;
import lombok.AllArgsConstructor;
import javax.inject.Named;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Sat, 26 Feb, 2022
 */
@Named
@AllArgsConstructor
public class CreateBusinessLoanUseCaseImpl implements CreateBusinessLoanUseCase {

    private final ApplicationProperty applicationProperty;
    private final LoanRequestEntityDao loanRequestEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final GetBusinessLoanUseCase getBusinessLoanUseCase;
    private final ApplicationEventService applicationEventService;

    @Override
    public BusinessLoanResponse createRequest(AuthenticatedUser authenticatedUser, BigDecimal loanAmount, int durationInMonths, String creditAccountId) {
        AppUserEntity currentUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(creditAccountId, accountEntity)
                .orElseThrow(() -> new BadRequestException("Invalid credit account Id"));

        long pendingLoanCount = loanRequestEntityDao.countPendingLoanRequest(currentUser, LoanTypeConstant.BUSINESS);
        if(pendingLoanCount > 0) {
            throw new BadRequestException("Sorry, you have a loan request pending review and approval.");
        }

        long activeLoanCount = loanRequestEntityDao.countActiveLoan(currentUser, LoanTypeConstant.BUSINESS);
        if(activeLoanCount > 0) {
            throw new BadRequestException("Sorry, you already have an active loan running.");
        }

        BigDecimal loanInterest = loanAmount.multiply(BigDecimal.valueOf(applicationProperty.getBusinessLoanInterestRate() / 100.0));

        loanInterest = BigDecimal.valueOf(loanInterest.doubleValue() * durationInMonths);

        LoanRequestEntity loanRequestEntity = LoanRequestEntity.builder()
                .bankAccount(creditAccount)
                .loanId(loanRequestEntityDao.generateLoanId())
                .interestRate(applicationProperty.getBusinessLoanInterestRate())
                .loanAmount(loanAmount)
                .repaymentAmount(loanAmount.add(loanInterest))
                .activeLoan(true)
                .requestedBy(currentUser)
                .loanInterest(loanInterest)
                .loanType(LoanTypeConstant.BUSINESS)
                .durationInMonths(durationInMonths)
                .build();
        loanRequestEntity = loanRequestEntityDao.saveRecord(loanRequestEntity);

        LoanEmailEvent loanEmailEvent = LoanEmailEvent.builder()
                .customerName(currentUser.getName())
                .recipient(currentUser.getEmail())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REQUEST_SUCCESS, new EventModel<>(loanEmailEvent));

        loanEmailEvent = LoanEmailEvent.builder()
                .recipient(applicationProperty.getLoanAdminEmail())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REQUEST_ADMIN, new EventModel<>(loanEmailEvent));

        return getBusinessLoanUseCase.fromEntityToResponse(loanRequestEntity);
    }
}
