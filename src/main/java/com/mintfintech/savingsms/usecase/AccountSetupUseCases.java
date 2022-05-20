package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.usecase.data.events.incoming.*;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
public interface AccountSetupUseCases {
    void createMintAccount(MintAccountCreationEvent mintAccountCreationEvent);
    void createIndividualBankAccount(MintBankAccountCreationEvent accountCreationEvent);
  //  void updateAccountTransactionLimit(AccountLimitUpdateEvent accountLimitUpdateEvent);
    void createMintUser(UserCreationEvent userCreationEvent);
    void updateBankAccountTierLevel(BankAccountTierUpgradeEvent accountTierUpgradeEvent);
    void updateNotificationPreference(NotificationPreferenceUpdateEvent preferenceUpdateEvent);
    void updateUserDeviceNotificationId(String userId, String gcmTokenId);
    void updateBankAccountStatus(BankAccountStatusUpdateEvent accountStatusUpdateEvent);
    void updateUserProfileDetails(UserDetailUpdateEvent updateEvent);
    void createOrUpdateCorporateUser(CorporateUserDetailEvent corporateUserDetailEvent);
}
