package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerLoanProfileUseCase {

    LoanCustomerProfileModel addCustomerEmploymentInformation(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request);

    LoanCustomerProfileModel verifyEmploymentInformation(AuthenticatedUser currentUser, long customerLoanProfileId);

    LoanCustomerProfileModel blackListCustomer(AuthenticatedUser currentUser, long customerLoanProfileId, String reason);

    BigDecimal getLoanMaxAmount(AuthenticatedUser currentUser, String loanType);

    List<LoanCustomerProfileModel> getLoanCustomerProfiles(boolean blacklisted, boolean employeeInfoVerified);

 }
