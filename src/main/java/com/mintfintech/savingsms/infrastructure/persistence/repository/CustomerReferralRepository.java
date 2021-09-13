package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.CustomerReferralEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.models.reports.ReferralRewardStat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 15 Dec, 2020
 */
public interface CustomerReferralRepository extends JpaRepository<CustomerReferralEntity, Long> {
    boolean existsByReferred(MintAccountEntity referred);
    boolean existsByReferrerAndReferred(MintAccountEntity referral, MintAccountEntity referred);
    Optional<CustomerReferralEntity> findFirstByReferredAndReferredRewardedFalse(MintAccountEntity referred);
    long countAllByReferrer(MintAccountEntity mintAccountEntity);
    Optional<CustomerReferralEntity> findFirstByReferredAndReferralCodeIgnoreCase(MintAccountEntity referred, String referralCode);


    List<CustomerReferralEntity> getAllByReferrerAndReferrerRewardedAndDateCreatedBetweenOrderByDateCreatedDesc(MintAccountEntity referral,
                                                                       boolean rewarded,
                                                                       LocalDateTime start,
                                                                       LocalDateTime end,
                                                                       Pageable pageable);


    @Query("select c from CustomerReferralEntity c where c.dateCreated between ?1 and ?2 and c.referrerRewarded = false and " +
            "c.registrationPlatform <> 'WEB' and c.referred in (select s.mintAccount from SavingsGoalEntity s where " +
            "s.savingsBalance >= ?3 and s.mintAccount = c.referred and " +
            "s.savingsGoalType = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant.CUSTOMER_SAVINGS) order by c.dateCreated asc")
    List<CustomerReferralEntity> getUnprocessedReferrals(LocalDateTime startDate, LocalDateTime endDate, BigDecimal savingsMinBalance, Pageable pageable);

    @Query("select new com.mintfintech.savingsms.domain.models.reports.ReferralRewardStat(c.referrerRewarded, count(c)) from CustomerReferralEntity c " +
            "where c.referrer = ?1 group by c.referrerRewarded")
    List<ReferralRewardStat> getReferralStatisticsForAccount(MintAccountEntity referral);
}
