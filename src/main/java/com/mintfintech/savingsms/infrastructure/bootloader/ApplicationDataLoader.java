package com.mintfintech.savingsms.infrastructure.bootloader;

import com.mintfintech.savingsms.infrastructure.persistence.repository.MintAccountRepository;
import com.mintfintech.savingsms.usecase.master_record.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Slf4j
@Component
public class ApplicationDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private TierLevelDataUseCase tierLevelDataUseCase;
    private CurrencyDataUseCases currencyDataUseCases;
    private SavingsPlanUseCases savingsPlanUseCases;
    private SavingsGoalCategoryUseCase savingsGoalCategoryUseCase;
    private MintAccountRepository mintAccountRepository;
    /*private CoreBankingRestClient coreBankingRestClient;
    @Autowired
    public void setCoreBankingRestClient(CoreBankingRestClient coreBankingRestClient) {
        this.coreBankingRestClient = coreBankingRestClient;
    }*/

    public ApplicationDataLoader(TierLevelDataUseCase tierLevelDataUseCase, CurrencyDataUseCases currencyDataUseCases,
                                 SavingsPlanUseCases savingsPlanUseCases, SavingsGoalCategoryUseCase savingsGoalCategoryUseCase) {
        this.tierLevelDataUseCase = tierLevelDataUseCase;
        this.currencyDataUseCases = currencyDataUseCases;
        this.savingsPlanUseCases = savingsPlanUseCases;
        this.savingsGoalCategoryUseCase = savingsGoalCategoryUseCase;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        new Thread(() -> {
            tierLevelDataUseCase.createDefaultTierLevels();
            currencyDataUseCases.createDefaultRecords();
            savingsPlanUseCases.createDefaultSavingsPlan();
            savingsGoalCategoryUseCase.createDefaultSavingsCategory();
        } ).start();
        log.info("Application started");
        issueFix();

        /*long amount = 50000;
        BigDecimal longAmount = BigDecimal.valueOf(amount);
        BigDecimal doubleAmount = BigDecimal.valueOf(50000.00);
        int value = longAmount.compareTo(doubleAmount);
        System.out.println("value: "+value);*/

    }

    private void issueFix() {
        long totalCount = mintAccountRepository.countMintAccountsWithoutSavingGoals();
        System.out.println("MINT ACCOUNTS WITHOUT GOALS: "+totalCount);
    }
}
