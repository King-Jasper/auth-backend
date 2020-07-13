package com.mintfintech.savingsms.infrastructure.bootloader;

import com.google.gson.Gson;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.AppUserRepository;
import com.mintfintech.savingsms.infrastructure.persistence.repository.MintAccountRepository;
import com.mintfintech.savingsms.usecase.ApplySavingsInterestUseCase;
import com.mintfintech.savingsms.usecase.CreateSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalFundingEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalFundingFailureEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalWithdrawalSuccessEvent;
import com.mintfintech.savingsms.usecase.data.value_objects.EmailNotificationType;
import com.mintfintech.savingsms.usecase.master_record.*;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@FieldDefaults(makeFinal = true)
@Slf4j
@Component
@AllArgsConstructor
public class ApplicationDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private TierLevelDataUseCase tierLevelDataUseCase;
    private CurrencyDataUseCases currencyDataUseCases;
    private SavingsPlanUseCases savingsPlanUseCases;
    private SavingsGoalCategoryUseCase savingsGoalCategoryUseCase;
    private ApplySavingsInterestUseCase applySavingsInterestUseCase;
    private Gson gson;
   // private MintAccountRepository mintAccountRepository;
   // private CreateSavingsGoalUseCase createSavingsGoalUseCase;
   // private AppUserRepository appUserRepository;
    /*private CoreBankingRestClient coreBankingRestClient;
    @Autowired
    public void setCoreBankingRestClient(CoreBankingRestClient coreBankingRestClient) {
        this.coreBankingRestClient = coreBankingRestClient;
    }*/


    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        new Thread(() -> {
            tierLevelDataUseCase.createDefaultTierLevels();
            currencyDataUseCases.createDefaultRecords();
            savingsPlanUseCases.createDefaultSavingsPlan();
            savingsPlanUseCases.createDefaultSavingsTenor();
            savingsGoalCategoryUseCase.createDefaultSavingsCategory();
        } ).start();
        log.info("Application started");
       //applySavingsInterestUseCase.updateInterestLiabilityAccountWithAccumulatedInterest(BigDecimal.valueOf(0.04));
       //issueFix();
        /*long amount = 50000;
        BigDecimal longAmount = BigDecimal.valueOf(amount);
        BigDecimal doubleAmount = BigDecimal.valueOf(50000.00);
        int value = longAmount.compareTo(doubleAmount);
        System.out.println("value: "+value);*/
    }

    /*private void issueFix() {
        long totalCount = mintAccountRepository.countMintAccountsWithoutSavingGoals();
        System.out.println("MINT ACCOUNTS WITHOUT GOALS: "+totalCount);
        List<MintAccountEntity>  mintAccountEntityList = mintAccountRepository.mintAccountsWithoutSavingGoals();
        for(MintAccountEntity mintAccountEntity : mintAccountEntityList) {
            AppUserEntity appUserEntity = appUserRepository.getFirstByPrimaryAccount(mintAccountEntity);
            createSavingsGoalUseCase.createDefaultSavingsGoal(mintAccountEntity, appUserEntity);
        }
        totalCount = mintAccountRepository.countMintAccountsWithoutSavingGoals();
        System.out.println("MINT ACCOUNTS WITHOUT GOALS: "+totalCount);
    }*/
}
