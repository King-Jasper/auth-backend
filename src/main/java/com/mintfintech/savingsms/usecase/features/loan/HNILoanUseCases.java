package com.mintfintech.savingsms.usecase.features.loan;

import com.mintfintech.savingsms.domain.models.PagedResponse;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.HNICustomerCreationRequest;
import com.mintfintech.savingsms.usecase.data.request.HNICustomerSearchRequest;
import com.mintfintech.savingsms.usecase.models.HNILoanCustomerModel;
/**
 * Created by jnwanya on
 * Tue, 26 Sep, 2023
 */
public interface HNILoanUseCases {
   PagedResponse<HNILoanCustomerModel> getHNICustomers(HNICustomerSearchRequest searchRequest, int page, int size);
   HNILoanCustomerModel createHNICustomer(AuthenticatedUser authenticatedUser, HNICustomerCreationRequest creationRequest);
}
