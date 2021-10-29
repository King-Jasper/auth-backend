package com.mintfintech.savingsms.usecase.features.corporate;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;

public interface ManageTransactionRequestUseCase {
    String processApproval(AuthenticatedUser authenticatedUser, CorporateApprovalRequest request);
}
