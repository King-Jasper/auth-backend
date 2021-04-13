package com.mintfintech.savingsms.infrastructure.bootloader;

import com.google.gson.Gson;
import com.mintfintech.savingsms.domain.dao.AppSequenceEntityDao;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsGoalRepository;
import com.mintfintech.savingsms.usecase.ApplySavingsInterestUseCase;
import com.mintfintech.savingsms.usecase.master_record.CurrencyDataUseCases;
import com.mintfintech.savingsms.usecase.master_record.SavingsGoalCategoryUseCase;
import com.mintfintech.savingsms.usecase.master_record.SavingsPlanUseCases;
import com.mintfintech.savingsms.usecase.master_record.TierLevelDataUseCase;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

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
    private SavingsGoalRepository repository;
    private Gson gson;
    private AppSequenceEntityDao appSequenceEntityDao;
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
        /*
        new Thread(() -> {
            long id = appSequenceEntityDao.getNextSequenceId(SequenceType.SAVINGS_GOAL_SEQ);
            System.out.println("First thread id - "+id);
        }).start();
        new Thread(() -> {
            long id = appSequenceEntityDao.getNextSequenceId(SequenceType.SAVINGS_GOAL_SEQ);
            System.out.println("2nd thread id - "+id);
        }).start();
        new Thread(() -> {
            long id = appSequenceEntityDao.getNextSequenceId(SequenceType.SAVINGS_GOAL_SEQ);
            System.out.println("3rd thread id - "+id);
        }).start();
        */

       //applySavingsInterestUseCase.updateInterestLiabilityAccountWithAccumulatedInterest(BigDecimal.valueOf(0.04));
       //issueFix();
        /*long amount = 50000;
        BigDecimal longAmount = BigDecimal.valueOf(amount);
        BigDecimal doubleAmount = BigDecimal.valueOf(50000.00);
        int value = longAmount.compareTo(doubleAmount);
        System.out.println("value: "+value);*/
        //updateValue();
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
