package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.EmploymentDetailCreationRequest;
import com.mintfintech.savingsms.usecase.models.PayDayLoanCustomerProfileModel;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerLoanProfileUseCase {

    PayDayLoanCustomerProfileModel createCustomerProfile(AuthenticatedUser currentUser, EmploymentDetailCreationRequest request);

    PayDayLoanCustomerProfileModel verifyEmploymentInformation(AuthenticatedUser currentUser, long customerLoanProfileId);

    PayDayLoanCustomerProfileModel blackListCustomer(AuthenticatedUser currentUser, long customerLoanProfileId, String reason);

    BigDecimal getPayDayLoanMaxAmount(AuthenticatedUser currentUser);

    List<PayDayLoanCustomerProfileModel> getUnverifiedEmployeeInformation();

    List<PayDayLoanCustomerProfileModel> getVerifiedEmployeeInformation();
}
