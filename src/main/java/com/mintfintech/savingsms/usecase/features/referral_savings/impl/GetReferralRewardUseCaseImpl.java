package com.mintfintech.savingsms.usecase.features.referral_savings.impl;

import com.mintfintech.savingsms.domain.dao.CustomerReferralEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.CustomerReferralEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.ReferralDetailsResponse;
import com.mintfintech.savingsms.usecase.features.referral_savings.GetReferralRewardUseCase;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Named
@AllArgsConstructor
public class GetReferralRewardUseCaseImpl implements GetReferralRewardUseCase {

    private MintAccountEntityDao mintAccountEntityDao;
    private CustomerReferralEntityDao customerReferralEntityDao;
    private ApplicationProperty applicationProperty;

    @Override
    public ReferralDetailsResponse getReferralDetails(AuthenticatedUser authenticatedUser) {
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        LocalDateTime start = LocalDate.of(2022, 5, 14).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        BigDecimal totalEarnings = BigDecimal.ZERO;

        List<CustomerReferralEntity> referralList = customerReferralEntityDao.getProcessedReferralsByReferrer(mintAccount, start, end);
        int numberOfReferred = referralList.size();
        if (!referralList.isEmpty()) {
            for(CustomerReferralEntity customerReferral : referralList) {
                if(customerReferral.getReferrerRewardAmount() != null) {
                    totalEarnings = totalEarnings.add(customerReferral.getReferrerRewardAmount());
                }
            }
        }
        return ReferralDetailsResponse.builder()
                .totalEarnings(totalEarnings)
                .numberOfCustomersReferred(numberOfReferred)
                .referredAirtimeAmount(applicationProperty.getReferredAirtimeAmount())
                .referrerAmount(applicationProperty.getReferralRewardAmount())
                .build();
    }
}
