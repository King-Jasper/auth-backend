package com.mintfintech.savingsms.usecase.features.loan;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.CustomerProfileSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;

public interface CustomerLoanProfileUseCase {

    LoanCustomerProfileModel createPaydayCustomerLoanProfile(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request);

    LoanCustomerProfileModel updateCustomerEmploymentInformation(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request);

    LoanCustomerProfileModel verifyEmploymentInformation(AuthenticatedUser currentUser, long customerLoanProfileId, boolean isVerified, String reason);

    LoanCustomerProfileModel blackListCustomer(AuthenticatedUser currentUser, long customerLoanProfileId, boolean blacklist, String reason);

    LoanCustomerProfileModel getLoanCustomerProfile(AuthenticatedUser currentUser, String loanType);

    PagedDataResponse<LoanCustomerProfileModel> getPagedLoanCustomerProfiles(CustomerProfileSearchRequest request, int page, int size);

    LoanCustomerProfileModel getCustomerEmployerInfo(long customerLoanProfileId);

    LoanCustomerProfileModel toLoanCustomerProfileModel(CustomerLoanProfileEntity customerLoanProfileEntity);

    void updateCustomerRating(AppUserEntity currentUser);
 }
