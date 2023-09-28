package com.mintfintech.savingsms.usecase.features.loan.business_loan.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SettingsNameTypeConstant;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.cloudstorageservice.FileStorageRequest;
import com.mintfintech.savingsms.domain.models.cloudstorageservice.FileStorageResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.CloudStorageService;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.events.outgoing.LoanEmailEvent;
import com.mintfintech.savingsms.usecase.data.response.BusinessLoanResponse;
import com.mintfintech.savingsms.usecase.data.response.HairFinanceLoanResponse;
import com.mintfintech.savingsms.usecase.data.response.LoanRequestScheduleResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.features.loan.business_loan.CreateBusinessLoanUseCase;
import com.mintfintech.savingsms.usecase.features.loan.business_loan.GetBusinessLoanUseCase;
import com.mintfintech.savingsms.utils.MintStringUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Named;
import java.math.BigDecimal;
import java.util.Arrays;

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
    private final SettingsEntityDao settingsEntityDao;
    private final HNILoanCustomerEntityDao hniLoanCustomerEntityDao;
    private final LoanRepaymentScheduleEntityDao loanRepaymentScheduleEntityDao;
    private final CloudStorageService cloudStorageService;

    @Override
    public BusinessLoanResponse createRequest(AuthenticatedUser authenticatedUser, BigDecimal loanAmount, int durationInMonths, String creditAccountId) {
        AppUserEntity currentUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(creditAccountId, accountEntity)
                .orElseThrow(() -> new BadRequestException("Invalid credit account Id"));

        boolean accessBusinessLoan = false;
        String accounts = settingsEntityDao.getSettings(SettingsNameTypeConstant.BUSINESS_LOAN_ACCESS_ACCOUNT_IDS, "");
        if(StringUtils.isNotEmpty(accounts)) {
            accessBusinessLoan = Arrays.stream(accounts.split(":")).anyMatch(data -> data.equalsIgnoreCase(authenticatedUser.getAccountId()));
        }
        if(!accessBusinessLoan) {
            throw new BadRequestException("Sorry, you are not yet qualified for a business loan.");
        }

        if(applicationProperty.isLiveEnvironment()) {
            long pendingLoanCount = loanRequestEntityDao.countPendingLoanRequest(currentUser, LoanTypeConstant.BUSINESS);
            if(pendingLoanCount > 0) {
                throw new BadRequestException("Sorry, you have a loan request pending review and approval.");
            }

            long activeLoanCount = loanRequestEntityDao.countActiveLoan(currentUser, LoanTypeConstant.BUSINESS);
            if(activeLoanCount > 0) {
                throw new BadRequestException("Sorry, you already have an active loan running.");
            }
        }

        String rateString = settingsEntityDao.getSettings(SettingsNameTypeConstant.BUSINESS_LOAN_RATE, "4.0");
        double businessRate = Double.parseDouble(rateString);

        BigDecimal loanInterest = loanAmount.multiply(BigDecimal.valueOf(businessRate / 100.0));

        loanInterest = BigDecimal.valueOf(loanInterest.doubleValue() * durationInMonths);

        LoanRequestEntity loanRequestEntity = LoanRequestEntity.builder()
                .bankAccount(creditAccount)
                .loanId(loanRequestEntityDao.generateLoanId())
                .interestRate(businessRate)
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

    @Override
    @Transactional
    public BusinessLoanResponse createRequest(AuthenticatedUser authenticatedUser, BigDecimal loanAmount, int durationInMonths, String creditAccountId, MultipartFile chequeFile) {

        AppUserEntity currentUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());

        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(creditAccountId, accountEntity)
                .orElseThrow(() -> new BadRequestException("Invalid credit account Id"));

        if(loanAmount.doubleValue() < 10000){
            throw new BusinessLogicConflictException("Minimum loan amount is 10,000");
        }

        if(durationInMonths < 1) {
            throw new BadRequestException("Minimum duration is 1 month");
        }

        HNILoanCustomerEntity hniLoanCustomer = hniLoanCustomerEntityDao.findRecord(accountEntity)
                .orElseThrow(() -> new BusinessLogicConflictException("Sorry, you have not been granted access to business loan."));

        if(hniLoanCustomer.isChequeRequired() && chequeFile == null) {
            throw new BadRequestException("Sorry, a post dated cheque is required for your application.");
        }

        /*
        if(applicationProperty.isLiveEnvironment()) {
            long pendingLoanCount = loanRequestEntityDao.countPendingLoanRequest(currentUser, LoanTypeConstant.BUSINESS);
            if(pendingLoanCount > 0) {
                throw new BadRequestException("Sorry, you have a loan request pending review and approval.");
            }

            long activeLoanCount = loanRequestEntityDao.countActiveLoan(currentUser, LoanTypeConstant.BUSINESS);
            if(activeLoanCount > 0) {
                throw new BadRequestException("Sorry, you already have an active loan running.");
            }
        }*/

        long pendingLoanCount = loanRequestEntityDao.countPendingLoanRequest(currentUser, LoanTypeConstant.BUSINESS);
        if(pendingLoanCount > 0) {
            throw new BadRequestException("Sorry, you have a loan request pending review and approval.");
        }

        long activeLoanCount = loanRequestEntityDao.countActiveLoan(currentUser, LoanTypeConstant.BUSINESS);
        if(activeLoanCount > 0) {
            throw new BadRequestException("Sorry, you already have an active loan running.");
        }

        String postDateChequeUrl = "";
        if(chequeFile != null) {
            byte[] fileData = null;
            try {
                fileData = chequeFile.getBytes();
            }catch (Exception ignored){}

            String fileName = chequeFile.getOriginalFilename();
            FileStorageRequest storageRequest = FileStorageRequest.builder()
                    .fileName(fileName)
                    .fileData(fileData)
                    .folderName("business_loan")
                    .privateFile(false)
                    .build();
            FileStorageResponse storageResponse = cloudStorageService.uploadResourceFile(storageRequest);
            if(!storageResponse.isSuccess()) {
                throw new BusinessLogicConflictException("Sorry, unable to upload file. Please try again later.");
            }
            postDateChequeUrl = storageResponse.getUrl();
        }

        LoanRequestScheduleResponse scheduleResponse = getBusinessLoanUseCase.getRepaymentSchedule(hniLoanCustomer, loanAmount, durationInMonths);
        BigDecimal repaymentAmount = scheduleResponse.getRepaymentAmount();

        LoanRequestEntity loanRequestEntity = LoanRequestEntity.builder()
                .bankAccount(creditAccount)
                .loanId(loanRequestEntityDao.generateLoanId())
                .interestRate(hniLoanCustomer.getInterestRate())
                .loanAmount(loanAmount)
                .repaymentAmount(repaymentAmount)
                .activeLoan(true)
                .requestedBy(currentUser)
                .loanInterest(repaymentAmount.subtract(loanAmount))
                .loanType(LoanTypeConstant.BUSINESS)
                .durationInMonths(durationInMonths)
                .hniLoanCustomer(hniLoanCustomer)
                .repaymentPlanType(hniLoanCustomer.getRepaymentPlanType())
                .postDatedChequeUrl(postDateChequeUrl)
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

    @Override
    public HairFinanceLoanResponse createHairFinanceLoanRequest(AuthenticatedUser authenticatedUser, BigDecimal loanAmount, int durationInMonths, String creditAccountId) {
        MintAccountEntity accountEntity = mintAccountEntityDao.findAccountByAccountId(authenticatedUser.getAccountId())
                .orElseThrow(() -> new BadRequestException("Invalid merchant account Id."));
        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(creditAccountId, accountEntity)
                .orElseThrow(() -> new BadRequestException("Invalid merchant bank account Id"));

        if(!creditAccount.getMintAccount().getId().equals(accountEntity.getId())) {
            throw new BadRequestException("Invalid merchant bank account Id");
        }

        String rateString = settingsEntityDao.getSettings(SettingsNameTypeConstant.HAIR_FINANCE_LOAN_RATE, "4.0");
        double businessRate = Double.parseDouble(rateString);

        BigDecimal loanInterest = loanAmount.multiply(BigDecimal.valueOf(businessRate / 100.0));

        loanInterest = BigDecimal.valueOf(loanInterest.doubleValue() * durationInMonths);

        LoanRequestEntity loanRequestEntity = LoanRequestEntity.builder()
                .bankAccount(creditAccount)
                .loanId(loanRequestEntityDao.generateLoanId())
                .interestRate(businessRate)
                .loanAmount(loanAmount)
                .repaymentAmount(loanAmount.add(loanInterest))
                .activeLoan(true)
                .requestedBy(accountEntity.getCreator())
                .loanInterest(loanInterest)
                .loanType(LoanTypeConstant.HAIR_FINANCE)
                .durationInMonths(durationInMonths)
                .build();
        loanRequestEntity = loanRequestEntityDao.saveRecord(loanRequestEntity);

        LoanEmailEvent loanEmailEvent = LoanEmailEvent.builder()
                .recipient(applicationProperty.getLoanAdminEmail())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_LOAN_REQUEST_ADMIN, new EventModel<>(loanEmailEvent));

        return getBusinessLoanUseCase.fromEntityToHairFinanceResponse(loanRequestEntity);
    }
}
