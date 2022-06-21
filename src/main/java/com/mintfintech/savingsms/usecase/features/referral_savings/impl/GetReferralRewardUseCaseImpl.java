package com.mintfintech.savingsms.usecase.features.referral_savings.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.CustomerReferralEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerReferralEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.AccountsRestClient;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.UserDetailUpdateEvent;
import com.mintfintech.savingsms.usecase.data.response.ReferralDetailsResponse;
import com.mintfintech.savingsms.usecase.features.referral_savings.GetReferralRewardUseCase;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Named
@AllArgsConstructor
public class GetReferralRewardUseCaseImpl implements GetReferralRewardUseCase {

    private MintAccountEntityDao mintAccountEntityDao;
    private AppUserEntityDao appUserEntityDao;
    private CustomerReferralEntityDao customerReferralEntityDao;
    private ApplicationProperty applicationProperty;
    private AccountsRestClient accountsRestClient;
    private final SavingsGoalEntityDao savingsGoalEntityDao;
    private GetSavingsGoalUseCase getSavingsGoalUseCase;

    @Override
    public ReferralDetailsResponse getReferralDetails(AuthenticatedUser authenticatedUser) {
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());

        LocalDateTime start = LocalDate.of(2022, 6, 19).atStartOfDay();
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
        String username = appUser.getUsername();
        if(StringUtils.isEmpty(username)) {
            MsClientResponse<UserDetailUpdateEvent> msClientResponse = accountsRestClient.getUserDetails(appUser.getUserId());
            if(msClientResponse.isSuccess()) {
                UserDetailUpdateEvent updateEvent = msClientResponse.getData();
                username = updateEvent.getUsername();
                appUser.setUsername(username);
                appUserEntityDao.saveRecord(appUser);
            }
        }
        BigDecimal availableBalance = BigDecimal.ZERO;
        SavingsGoalModel referralPurse = null;
        Optional<SavingsGoalEntity> goalEntityOpt = savingsGoalEntityDao.findFirstSavingsByTypeIgnoreStatus(mintAccount, SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS);
        if(goalEntityOpt.isPresent()) {
            referralPurse = getSavingsGoalUseCase.fromSavingsGoalEntityToModel(goalEntityOpt.get());
            availableBalance = referralPurse.getSavingsBalance();
        }
        /*
        String message = "Get 2,000 Naira when three(3) of your friends open a free Mintyn current account using your code - "+username.toUpperCase()+".\n\n" +
                "Your friend will get 300 Naira free airtime top-up, using your code.";
        */
        String message = "We are coming back big and better.";
        return ReferralDetailsResponse.builder()
                .totalEarnings(totalEarnings)
                .numberOfCustomersReferred(numberOfReferred)
                .referredAirtimeAmount(300)
                .referrerAmount(2000)
                .referralMessage(message)
                .availableBalance(availableBalance)
                .referralPurse(referralPurse)
                .build();
    }
}
