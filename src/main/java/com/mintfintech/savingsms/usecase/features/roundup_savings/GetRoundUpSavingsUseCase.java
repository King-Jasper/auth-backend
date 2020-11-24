package com.mintfintech.savingsms.usecase.features.roundup_savings;

import com.mintfintech.savingsms.domain.entities.RoundUpSavingsSettingEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.RoundUpSavingResponse;
import com.mintfintech.savingsms.usecase.models.RoundUpSavingsTransactionModel;

/**
 * Created by jnwanya on
 * Sat, 31 Oct, 2020
 */
public interface GetRoundUpSavingsUseCase {
    RoundUpSavingResponse fromEntityToResponse(RoundUpSavingsSettingEntity roundUpSavingsSettingEntity);
    RoundUpSavingResponse getAccountRoundUpSavings(AuthenticatedUser authenticatedUser);
    PagedDataResponse<RoundUpSavingsTransactionModel> getRoundUpSavingsTransaction(AuthenticatedUser authenticatedUser, Long roundUpId, int pageIndex, int size);
}
