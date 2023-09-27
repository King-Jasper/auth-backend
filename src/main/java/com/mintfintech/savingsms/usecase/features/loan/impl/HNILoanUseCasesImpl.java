package com.mintfintech.savingsms.usecase.features.loan.impl;

import com.mintfintech.savingsms.domain.dao.HNILoanCustomerEntityDao;
import com.mintfintech.savingsms.domain.dao.LoanRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.HNILoanCustomerEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentPlanTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.models.PagedResponse;
import com.mintfintech.savingsms.domain.models.reports.HNICustomerSearchDTO;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.HNICustomerCreationRequest;
import com.mintfintech.savingsms.usecase.data.request.HNICustomerSearchRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.loan.HNILoanUseCases;
import com.mintfintech.savingsms.usecase.models.HNILoanCustomerModel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;

import javax.inject.Named;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by jnwanya on
 * Tue, 26 Sep, 2023
 */
@Named
@RequiredArgsConstructor
public class HNILoanUseCasesImpl implements HNILoanUseCases {

    private final HNILoanCustomerEntityDao hniLoanCustomerEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final LoanRequestEntityDao loanRequestEntityDao;

    @Override
    public PagedResponse<HNILoanCustomerModel> getHNICustomers(HNICustomerSearchRequest searchRequest, int page, int size) {
        HNICustomerSearchDTO searchDTO = HNICustomerSearchDTO.builder()
                .build();
        if(StringUtils.isNotEmpty(searchRequest.getCustomerNameOrAccountNumber())) {
            String value = searchRequest.getCustomerNameOrAccountNumber();
            boolean atLeastOneAlpha = value.matches(".*[a-zA-Z]+.*");
            if(atLeastOneAlpha) {
                searchDTO.setCustomerName(value);
            }else{
                if(value.length() != 10) {
                    throw new BadRequestException("Account number must be 10 digits.");
                }
                Optional<MintBankAccountEntity> bankAccountOpt = mintBankAccountEntityDao.findByAccountNumber(value);
                if(bankAccountOpt.isEmpty()) {
                    throw new BadRequestException("Incorrect account number. No record found.");
                }
                searchDTO.setMintAccount(bankAccountOpt.get().getMintAccount());
            }
        }
        if(StringUtils.isNotEmpty(searchRequest.getRepaymentType())) {
            searchDTO.setRepaymentPlanType(LoanRepaymentPlanTypeConstant.valueOf(searchRequest.getRepaymentType()));
        }
        Page<HNILoanCustomerEntity> entityPage = hniLoanCustomerEntityDao.getRecords(searchDTO, page, size);
        return new PagedResponse<>(entityPage.getTotalElements(), entityPage.getTotalPages(),
                entityPage.get().map(this::fromEntityToModel).collect(Collectors.toList()));
    }

    @Override
    public HNILoanCustomerModel createHNICustomer(AuthenticatedUser authenticatedUser, HNICustomerCreationRequest creationRequest) {
        Optional<MintBankAccountEntity> bankAccountOpt = mintBankAccountEntityDao.findByAccountNumber(creationRequest.getAccountNumber());
        if(bankAccountOpt.isEmpty()) {
            throw new BadRequestException("Incorrect account number. No record found.");
        }
        MintBankAccountEntity bankAccount = bankAccountOpt.get();
        long loanCount = loanRequestEntityDao.countActiveLoan(bankAccount.getMintAccount(), LoanTypeConstant.BUSINESS);
        if(loanCount > 0) {
            throw new BusinessLogicConflictException("Request rejected. Customer has active loan("+loanCount+").");
        }
        HNILoanCustomerEntity hniLoanCustomer;
        Optional<HNILoanCustomerEntity> optional = hniLoanCustomerEntityDao.findRecord(bankAccount.getMintAccount());
        if(optional.isPresent()) {
            hniLoanCustomer = optional.get();
        }else {
            hniLoanCustomer = new HNILoanCustomerEntity();
            hniLoanCustomer.setCustomer(bankAccount.getMintAccount());
        }
        hniLoanCustomer.setChequeRequired(creationRequest.isChequeRequired());
        hniLoanCustomer.setInterestRate(creationRequest.getInterestRate());
        hniLoanCustomer.setRepaymentPlanType(LoanRepaymentPlanTypeConstant.valueOf(creationRequest.getRepaymentPlan()));
        hniLoanCustomer.setLastProfiledBy(authenticatedUser.getName());
        hniLoanCustomer =  hniLoanCustomerEntityDao.saveRecord(hniLoanCustomer);

        return fromEntityToModel(hniLoanCustomer);
    }


    private HNILoanCustomerModel fromEntityToModel(HNILoanCustomerEntity customer) {
        MintAccountEntity account = customer.getCustomer();
        MintBankAccountEntity mintBankAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(account, BankAccountTypeConstant.CURRENT);
        return HNILoanCustomerModel.builder()
                .chequeRequired(customer.isChequeRequired())
                .dateProfiled(customer.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .interestRate(customer.getInterestRate())
                .repaymentType(customer.getRepaymentPlanType().getType())
                .lastUpdatedBy(customer.getLastProfiledBy())
                .accountNumber(mintBankAccount.getAccountNumber())
                .customerName(account.getName())
                .build();
    }
}
