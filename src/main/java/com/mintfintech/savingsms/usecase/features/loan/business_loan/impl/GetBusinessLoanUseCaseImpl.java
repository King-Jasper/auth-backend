package com.mintfintech.savingsms.usecase.features.loan.business_loan.impl;

import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SettingsEntityDao;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SettingsNameTypeConstant;
import com.mintfintech.savingsms.domain.models.LoanSearchDTO;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.BusinessLoanResponse;
import com.mintfintech.savingsms.usecase.data.response.LoanRequestScheduleResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.RepaymentSchedule;
import com.mintfintech.savingsms.usecase.features.loan.business_loan.GetBusinessLoanUseCase;
import lombok.AllArgsConstructor;
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
@Named
@AllArgsConstructor
public class GetBusinessLoanUseCaseImpl implements GetBusinessLoanUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final LoanRequestEntityDao loanRequestEntityDao;
    private final ApplicationProperty applicationProperty;
    private final SettingsEntityDao settingsEntityDao;

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
        String rateString = settingsEntityDao.getSettings(SettingsNameTypeConstant.BUSINESS_LOAN_RATE, "4.0");
        double businessRate = Double.parseDouble(rateString);
        double monthlyInterest = amount.doubleValue() * (businessRate / 100.0);
        double totalInterest = monthlyInterest * duration;
        double totalRepaymentAmount = amount.doubleValue() + totalInterest;

        BigDecimal monthlyPrincipal = amount.divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_EVEN);

       // BigDecimal monthlyPayment = amount.add(BigDecimal.valueOf(monthlyInterest));
        List<RepaymentSchedule> schedules = new ArrayList<>();
        LocalDate date = LocalDate.now();

        for(int i = 0; i < duration; i++) {
            date = date.plusDays(30);
            schedules.add(new RepaymentSchedule(date.format(DateTimeFormatter.ISO_DATE),
                    monthlyPrincipal.add(BigDecimal.valueOf(monthlyInterest))));
        }
        return LoanRequestScheduleResponse.builder()
                .loanAmount(amount)
                .interestRate(businessRate)
                .repaymentAmount(amount.add(BigDecimal.valueOf(totalInterest)))
                .schedules(schedules)
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
}
