package com.mintfintech.savingsms.usecase.features.loan.business_loan.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.HNILoanCustomerEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentPlanTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SettingsNameTypeConstant;
import com.mintfintech.savingsms.domain.models.LoanSearchDTO;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.*;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.features.loan.business_loan.GetBusinessLoanUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;

import javax.inject.Named;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jnwanya on
 * Sat, 26 Feb, 2022
 */
@Slf4j
@Named
@AllArgsConstructor
public class GetBusinessLoanUseCaseImpl implements GetBusinessLoanUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final LoanRequestEntityDao loanRequestEntityDao;
    private final ApplicationProperty applicationProperty;
    private final SettingsEntityDao settingsEntityDao;
    private final HNILoanCustomerEntityDao hniLoanCustomerEntityDao;

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
    public LoanRequestScheduleResponse getRepaymentSchedule(AuthenticatedUser authenticatedUser, BigDecimal amount, int duration) {
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        HNILoanCustomerEntity hniLoanCustomer = hniLoanCustomerEntityDao.findRecord(mintAccount)
                .orElseThrow(() -> new BusinessLogicConflictException("Sorry, no HNI Loan record has been created for this request."));

        return getRepaymentSchedule(hniLoanCustomer, amount, duration);

        /*
        String rateString = settingsEntityDao.getSettings(SettingsNameTypeConstant.BUSINESS_LOAN_RATE, "4.0");
        double businessRate = Double.parseDouble(rateString);
        double monthlyInterest = amount.doubleValue() * (businessRate / 100.0);
        double totalInterest = monthlyInterest * duration;
        double totalRepaymentAmount = amount.doubleValue() + totalInterest;

        BigDecimal monthlyPrincipal = amount.divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_EVEN);
        BigDecimal totalPrincipal = BigDecimal.valueOf(amount.doubleValue());
       // BigDecimal monthlyPayment = amount.add(BigDecimal.valueOf(monthlyInterest));
        List<RepaymentSchedule> schedules = new ArrayList<>();
        LocalDate date = LocalDate.now();
        for(int i = 0; i < duration; i++) {
            date = date.plusDays(30);
            //schedules.add(new RepaymentSchedule(date.format(DateTimeFormatter.ISO_DATE), monthlyPrincipal.add(BigDecimal.valueOf(monthlyInterest))));
            if(i + 1 == duration) {
                schedules.add(new RepaymentSchedule(date.format(DateTimeFormatter.ISO_DATE), totalPrincipal.add(BigDecimal.valueOf(monthlyInterest))));
            }else {
                schedules.add(new RepaymentSchedule(date.format(DateTimeFormatter.ISO_DATE), BigDecimal.valueOf(monthlyInterest)));
            }
        }
        return LoanRequestScheduleResponse.builder()
                .loanAmount(amount)
                .interestRate(businessRate)
                .repaymentAmount(amount.add(BigDecimal.valueOf(totalInterest)))
                .schedules(schedules)
                .build();
        */
    }

    @Override
    public LoanRequestScheduleResponse getRepaymentSchedule(HNILoanCustomerEntity hniLoanCustomer, BigDecimal amount, int duration) {
        if(!Hibernate.isInitialized(hniLoanCustomer)) {
            hniLoanCustomer = hniLoanCustomerEntityDao.getRecordById(hniLoanCustomer.getId());
        }
        double businessRate = hniLoanCustomer.getInterestRate();
        BigDecimal totalPrincipal = BigDecimal.valueOf(amount.doubleValue());
        double monthlyInterest = amount.doubleValue() * (businessRate / 100.0);
        double totalInterest = monthlyInterest * duration;
        double totalRepaymentAmount = amount.doubleValue() + totalInterest;

        LoanRepaymentPlanTypeConstant repaymentPlanType = hniLoanCustomer.getRepaymentPlanType();
        List<RepaymentSchedule> schedules = new ArrayList<>();
        if(repaymentPlanType == LoanRepaymentPlanTypeConstant.END_OF_TENURE) {

            LocalDate date = LocalDate.now().plusMonths(duration);
            schedules.add(new RepaymentSchedule(date, totalPrincipal, BigDecimal.valueOf(totalInterest)));

        }else if(repaymentPlanType == LoanRepaymentPlanTypeConstant.INTEREST_ONLY) {
            LocalDate date = LocalDate.now();
            for(int i = 0; i < duration; i++) {
                date = date.plusDays(30);
                if(i + 1 == duration) {
                    schedules.add(new RepaymentSchedule(date, totalPrincipal, BigDecimal.valueOf(monthlyInterest)));
                }else {
                    schedules.add(new RepaymentSchedule(date, BigDecimal.ZERO, BigDecimal.valueOf(monthlyInterest)));
                }
            }

        }else if(repaymentPlanType == LoanRepaymentPlanTypeConstant.PRORATED_PRINCIPAL_INTEREST) {
            BigDecimal monthlyPrincipal = amount.divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_EVEN);
            LocalDate date = LocalDate.now();
            for(int i = 0; i < duration; i++) {
                date = date.plusDays(30);
                schedules.add(new RepaymentSchedule(date, monthlyPrincipal, BigDecimal.valueOf(monthlyInterest)));
            }
        }else {
            throw new BusinessLogicConflictException("Unknown repayment plan.");
        }
        return LoanRequestScheduleResponse.builder()
                .loanAmount(amount)
                .interestRate(businessRate)
                .repaymentAmount(BigDecimal.valueOf(totalRepaymentAmount))
                .schedules(schedules)
                .repaymentDueDate(schedules.get(schedules.size() - 1).getRepaymentDate())
                .build();
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
        }else {
            response.setDueDate(null);
        }
        return response;
    }

    @Override
    public HairFinanceLoanResponse fromEntityToHairFinanceResponse(LoanRequestEntity loanRequest) {
        if(!Hibernate.isInitialized(loanRequest)) {
            loanRequest = loanRequestEntityDao.getRecordById(loanRequest.getId());
        }
        HairFinanceLoanResponse response = HairFinanceLoanResponse.builder()
                .loanAmount(loanRequest.getLoanAmount())
                .loanId(loanRequest.getLoanId())
                .dateApplied(loanRequest.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .durationInMonths(loanRequest.getDurationInMonths())
                .approvalStatus(loanRequest.getApprovalStatus().name())
                .repaymentStatus(loanRequest.getRepaymentStatus().name())
                .repaymentAmount(loanRequest.getRepaymentAmount())
                .amountPaid(loanRequest.getAmountPaid())
                .interestRate(loanRequest.getInterestRate())
                .build();
        if(loanRequest.getApprovalStatus() == ApprovalStatusConstant.APPROVED) {
            response.setDateDisbursed(loanRequest.getApprovedDate().format(DateTimeFormatter.ISO_DATE_TIME));
            response.setDueDate(loanRequest.getRepaymentDueDate().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        return response;
    }

    @Override
    public HairFinanceLoanResponse getHairFinanceLoanDetail(AuthenticatedUser currentUser, String loanId) {
        LoanRequestEntity loanRequest = loanRequestEntityDao.findByLoanId(loanId).orElseThrow(() ->  new NotFoundException("Invalid Loan Id."));
       /* MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());
        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getRecordById(loanRequest.getBankAccount().getId());
        if(!mintAccount.getId().equals(creditAccount.getMintAccount().getId())) {
            log.info("Loan request does not belong to mint account");
            throw new NotFoundException("Invalid Loan Id.");
        }*/
        return fromEntityToHairFinanceResponse(loanRequest);
    }
}
