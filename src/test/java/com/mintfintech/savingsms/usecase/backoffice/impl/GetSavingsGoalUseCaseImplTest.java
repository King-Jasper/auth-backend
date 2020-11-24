package com.mintfintech.savingsms.usecase.backoffice.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.persistence.daoimpl.*;
import com.mintfintech.savingsms.infrastructure.persistence.repository.*;
import com.mintfintech.savingsms.usecase.ComputeAvailableAmountUseCase;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.request.SavingsSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.PortalSavingsGoalResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.impl.GetSavingsGoalUseCaseImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Wilson on
 * Aug. 23, 2020
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2,replace = AutoConfigureTestDatabase.Replace.ANY)
public class GetSavingsGoalUseCaseImplTest {

     private SavingsPlanTenorEntityDao savingsPlanTenorEntityDao;
    @Mock private SavingsPlanEntityDao savingsPlanEntityDao;
     private SavingsGoalEntityDao savingsGoalEntityDao;
     private MintAccountEntityDao mintAccountEntityDao;
     private AppUserEntityDao appUserEntityDao;
    @Mock private ApplicationProperty applicationProperty;
    @Mock private ComputeAvailableAmountUseCase computeAvailableAmountUseCase;
    @Mock private ApplicationEventService applicationEventService;
    @Mock private SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    @Mock private SavingsInterestEntityDao savingsInterestEntityDao;

    private AppSequenceEntityDao appSequenceEntityDao;
    private GetSavingsGoalUseCase getSavingsGoalUseCase;

    @Autowired AppSequenceRepository appSequenceRepository;
    @Autowired private MintAccountRepository mintAccountRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private SavingsPlanRepository savingsPlanRepository;
    @Autowired private SavingsPlanTenorRepository savingsPlanTenorRepository;
    @Autowired private SavingsGoalCategoryRepository savingsGoalCategoryRepository;
    @Autowired private SavingsGoalRepository savingsGoalRepository;



    private MintAccountEntity mintAccountEntity;
    private MintAccountEntity mintAccountEntity2;
    private AppUserEntity appUserEntity;
    private SavingsPlanEntity savingsPlanEntity;
    private SavingsPlanTenorEntity savingsPlanTenorEntity;
    private SavingsGoalCategoryEntity savingsGoalCategoryEntity;
    List<SavingsGoalEntity> savingsGoalEntities;

    @BeforeAll
    public void setUp(){
        mintAccountEntityDao = new MintAccountEntityDaoImpl(mintAccountRepository);
        appUserEntityDao = new AppUserEntityDaoImpl(appUserRepository);
        appSequenceEntityDao = new AppSequenceEntityDaoImpl(appSequenceRepository);
        savingsPlanTenorEntityDao = new SavingsPlanTenorEntityDaoImpl(savingsPlanTenorRepository);
        savingsPlanEntityDao = new SavingsPlanEntityDaoImpl(savingsPlanRepository,appSequenceEntityDao);
        savingsGoalEntityDao = new SavingsGoalEntityDaoImpl(savingsGoalRepository,appSequenceEntityDao);
        getSavingsGoalUseCase = new GetSavingsGoalUseCaseImpl(savingsPlanTenorEntityDao, savingsGoalTransactionEntityDao,
                savingsInterestEntityDao, savingsPlanEntityDao,savingsGoalEntityDao,mintAccountEntityDao,
                appUserEntityDao,applicationProperty,computeAvailableAmountUseCase,applicationEventService);


        mintAccountEntity = getMintAccount(1);
        mintAccountEntity2 = getMintAccount(2);
        appUserEntity = getAppUser();
        savingsPlanEntity = getSavingsPlanEntity();
        savingsPlanTenorEntity = getSavingsPlanTenorEntity();
        savingsGoalCategoryEntity = getSavingsGoalCategoryEntity();
        savingsGoalEntities = new ArrayList<>();
        createSavingGoals();
        createSavingGoals2();

    }
    @Test
    public void testEntityCreations(){
        Assertions.assertTrue(savingsGoalEntities.size() == 8);
    }
    @Test
    public void getActiveAllSavingsGoals_byFirstAccountId_thenGetTwo(){

        SavingsSearchRequest request = SavingsSearchRequest.builder()
                .autoSavedStatus("ALL")
                .savingsStatus("ACTIVE")
                .accountId("accountId1"+"1")
                .build();
        PagedDataResponse<PortalSavingsGoalResponse> response = getSavingsGoalUseCase.getPagedSavingsGoals(request,0,10);
        System.out.println("Size "+response.getRecords().size());
        Assertions.assertTrue(response.getRecords().size()==2);
    }
    @Test
    public void getAllActiveSavingsGoals_byFirstAccountId_thenGetOne(){

        SavingsSearchRequest request = SavingsSearchRequest.builder()
                .autoSavedStatus("ALL")
                .savingsStatus("ACTIVE")
                .accountId("accountId1"+"2")
                .build();
        PagedDataResponse<PortalSavingsGoalResponse> response = getSavingsGoalUseCase.getPagedSavingsGoals(request,0,10);
        System.out.println("Size "+response.getRecords().size());
        Assertions.assertTrue(response.getRecords().size()==1);
    }
    @Test
    public void getAllActiveSavingsGoals_byIncorrectAccountId_thenThrowsException(){

        SavingsSearchRequest request = SavingsSearchRequest.builder()
                .autoSavedStatus("ALL")
                .savingsStatus("ACTIVE")
                .accountId("accountId1"+"23")
                .build();
        Throwable throwable = Assertions.assertThrows(BadRequestException.class,()->getSavingsGoalUseCase.getPagedSavingsGoals(request,0,10));
        Assertions.assertTrue("Invalid account Id".equals(throwable.getMessage()));
    }
    private void createSavingGoals(){
        SavingsGoalEntity s1 = getSavingsGoal(1);
        savingsGoalEntities.add(savingsGoalRepository.save(s1));

        SavingsGoalEntity s2 = getSavingsGoal(2);
        s2.setGoalStatus(SavingsGoalStatusConstant.COMPLETED);
        s2.setDateCreated(LocalDateTime.now().minusWeeks(5));
        savingsGoalEntities.add(savingsGoalRepository.save(s2));

        SavingsGoalEntity s3 = getSavingsGoal(3);
        s3.setDateCreated(LocalDateTime.now().minusWeeks(1));
        s3.setGoalStatus(SavingsGoalStatusConstant.MATURED);
        savingsGoalEntities.add(savingsGoalRepository.save(s3));

        SavingsGoalEntity s4 = getSavingsGoal(4);
        s4.setGoalStatus(SavingsGoalStatusConstant.CANCELLED);
        savingsGoalEntities.add(savingsGoalRepository.save(s4));

        SavingsGoalEntity s5 = getSavingsGoal(5);
        s5.setDateCreated(LocalDateTime.now().minusWeeks(8));
        savingsGoalEntities.add(savingsGoalRepository.save(s5));

        SavingsGoalEntity s6 = getSavingsGoal(6);
        s6.setGoalStatus(SavingsGoalStatusConstant.MATURED);
        savingsGoalEntities.add(savingsGoalRepository.save(s6));
    }
    private void createSavingGoals2(){
        SavingsGoalEntity s1 = getSavingsGoal(8);
        s1.setMintAccount(mintAccountEntity2);
        savingsGoalEntities.add(savingsGoalRepository.save(s1));

        SavingsGoalEntity s2 = getSavingsGoal(9);
        s2.setMintAccount(mintAccountEntity2);
        s2.setGoalStatus(SavingsGoalStatusConstant.COMPLETED);
        s2.setDateCreated(LocalDateTime.now().minusWeeks(5));
        savingsGoalEntities.add(savingsGoalRepository.save(s2));

    }


    private SavingsGoalEntity getSavingsGoal(int index){
        return SavingsGoalEntity.builder()
                .goalId("goalId"+index)
                .mintAccount(mintAccountEntity)
                .creator(appUserEntity)
                .savingsPlan(savingsPlanEntity)
                .savingsPlanTenor(savingsPlanTenorEntity)
                .goalCategory(savingsGoalCategoryEntity)
                .creationSource(SavingsGoalCreationSourceConstant.CUSTOMER)
                .savingsGoalType(SavingsGoalTypeConstant.CUSTOMER_SAVINGS)
                .goalStatus(SavingsGoalStatusConstant.ACTIVE)
                .build();
    }
private SavingsGoalCategoryEntity getSavingsGoalCategoryEntity(){
    SavingsGoalCategoryEntity cat = SavingsGoalCategoryEntity.builder()
                .code("coded")
                .name("namesname")
                .build();
    return savingsGoalCategoryRepository.save(cat);
}
    private SavingsPlanTenorEntity getSavingsPlanTenorEntity(){
        SavingsPlanTenorEntity tenor = SavingsPlanTenorEntity.builder()
                .durationType(SavingsDurationTypeConstant.DAYS)
                .duration(237)
                .build();
        return savingsPlanTenorRepository.save(tenor);
    }
    private SavingsPlanEntity getSavingsPlanEntity(){
        SavingsPlanEntity plan = SavingsPlanEntity.builder()
                .description("Savings plan entity")
                .planId("planId")
                .planName(SavingsPlanTypeConstant.SAVINGS_TIER_ONE)
                .minimumBalance(BigDecimal.ZERO)
                .maximumBalance(BigDecimal.TEN)
                .build();
        return savingsPlanRepository.save(plan);
    }
    private AppUserEntity getAppUser(){
        AppUserEntity appUserEntity = AppUserEntity.builder()
                .userId("userId1")
                .primaryAccount(mintAccountEntity)
                .phoneNumber("08123456789")
                .name("AppUser Name")
                .email("appuser@gmail.com")
                .build();
        return appUserRepository.save(appUserEntity);

    }
    private MintAccountEntity getMintAccount(int index){
        MintAccountEntity m1 = MintAccountEntity.builder()
                .accountId("accountId1"+index)
                .name("Mint Account Name"+index)
                .build();
        return mintAccountRepository.save(m1);


    }

}
