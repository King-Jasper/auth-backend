package com.mintfintech.savingsms.usecase.features.loan;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.CustomerProfileSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.data.response.BusinessLoanInfo;
import com.mintfintech.savingsms.usecase.data.response.BusinessLoanResponse;
import com.mintfintech.savingsms.usecase.data.response.LoanDashboardResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.CustomerLoanProfileDashboard;
import com.mintfintech.savingsms.usecase.models.EmploymentInformationModel;
import com.mintfintech.savingsms.usecase.models.LoanCustomerProfileModel;

public interface CustomerLoanProfileUseCase {

    LoanDashboardResponse getLoanDashboardInformation(AuthenticatedUser authenticatedUser);

    LoanCustomerProfileModel createPaydayCustomerLoanProfile(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request);

    LoanCustomerProfileModel updateCustomerEmploymentInformation(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request);

    LoanCustomerProfileModel verifyEmploymentInformation(AuthenticatedUser currentUser, long customerLoanProfileId, boolean isVerified, String reason);

    LoanCustomerProfileModel blackListCustomer(AuthenticatedUser currentUser, long customerLoanProfileId, boolean blacklist, String reason);

    CustomerLoanProfileDashboard getLoanCustomerProfileDashboard(AuthenticatedUser currentUser, String loanType);

    PagedDataResponse<LoanCustomerProfileModel> getPagedLoanCustomerProfiles(CustomerProfileSearchRequest request, int page, int size);

    LoanCustomerProfileModel getCustomerEmployerInfo(long customerLoanProfileId);

    LoanCustomerProfileModel toLoanCustomerProfileModel(CustomerLoanProfileEntity customerLoanProfileEntity);

    LoanCustomerProfileModel getLoanProfileForBusinessLoan(LoanRequestEntity loanRequestEntity);

    void updateCustomerRating(AppUserEntity currentUser);

    EmploymentInformationModel getEmploymentInfo(AuthenticatedUser currentUser);
 }
