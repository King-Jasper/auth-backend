package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.CustomerLoanProfileEntityDao;
import com.mintfintech.savingsms.domain.dao.EmployeeInformationEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanTransactionEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import com.mintfintech.savingsms.domain.entities.EmployeeInformationEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.LoanTransactionEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.CustomerLoanProfileUseCase;
import com.mintfintech.savingsms.usecase.GetLoansUseCase;
import com.mintfintech.savingsms.usecase.LoanRequestUseCase;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;
import com.mintfintech.savingsms.usecase.models.LoanModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LoanRequestUseCaseImpl implements LoanRequestUseCase {

    private final LoanRequestEntityDao loanRequestEntityDao;
    private final ApplicationProperty applicationProperty;
    private final GetLoansUseCase getLoansUseCase;
    private final EmployeeInformationEntityDao employeeInformationEntityDao;
    private final CustomerLoanProfileEntityDao customerLoanProfileEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final CustomerLoanProfileUseCase customerLoanProfileUseCase;
    private final MintAccountEntityDao mintAccountEntityDao;

    @Override
    public LoanModel loanRequest(AuthenticatedUser currentUser, double amount, String loanType, String creditAccountId) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        List<MintBankAccountEntity> mintBankAccounts = mintBankAccountEntityDao.getAccountsByMintAccount(mintAccount);

        MintBankAccountEntity mintBankAccount = mintBankAccounts.get(0);

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
            throw new BadRequestException("There is an active loan for this user");
        }

        LoanRequestEntity loanRequestEntity = null;

        if (loanTypeConstant.equals(LoanTypeConstant.PAYDAY)) {
            loanRequestEntity = payDayLoanRequest(loanAmount, mintBankAccount, appUser);
        }

        return getLoansUseCase.toLoanModel(loanRequestEntity);

    }

    @Override
    @Transactional
    public LoanModel paydayLoanRequest(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request) {

        LoanCustomerProfileModel loanCustomerProfileModel = customerLoanProfileUseCase.createPaydayCustomerLoanProfile(currentUser, request);

        LoanModel loanModel = loanRequest(currentUser, request.getLoanAmount(), "PAYDAY", request.getCreditAccountId());
        loanModel.setOwner(loanCustomerProfileModel);

        return loanModel;
    }

    private LoanRequestEntity payDayLoanRequest(
            BigDecimal loanAmount,
            MintBankAccountEntity mintAccount,
            AppUserEntity appUser
    ) {

        BigDecimal loanInterest = loanAmount.multiply(BigDecimal.valueOf(applicationProperty.getPayDayLoanInterestRate() / 100.0));

        LoanRequestEntity loanRequestEntity = LoanRequestEntity.builder()
                .bankAccount(mintAccount)
                .loanId(loanRequestEntityDao.generateLoanId())
                .interestRate(applicationProperty.getPayDayLoanInterestRate())
                .loanAmount(loanAmount)
                .repaymentAmount(loanAmount.add(loanInterest))
                .requestedBy(appUser)
                .loanInterest(loanInterest)
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
