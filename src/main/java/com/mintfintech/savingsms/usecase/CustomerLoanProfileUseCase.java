package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;

public interface CustomerLoanProfileUseCase {

    LoanCustomerProfileModel payDayProfileCreationWithLoanRequest(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request);

    LoanCustomerProfileModel verifyEmploymentInformation(AuthenticatedUser currentUser, long customerLoanProfileId);

    LoanCustomerProfileModel blackListCustomer(AuthenticatedUser currentUser, long customerLoanProfileId, String reason);

    LoanCustomerProfileModel getLoanCustomerProfile(AuthenticatedUser currentUser, String loanType, String loanListType);

 }
