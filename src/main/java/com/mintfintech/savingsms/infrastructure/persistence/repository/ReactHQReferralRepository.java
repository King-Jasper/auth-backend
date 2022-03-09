package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.ReactHQReferralEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
/**
 * Created by jnwanya on
 * Thu, 03 Mar, 2022
 */
public interface ReactHQReferralRepository extends JpaRepository<ReactHQReferralEntity, Long> {

    @Query("select r from ReactHQReferralEntity r, MintBankAccountEntity mb where r.customer = mb.mintAccount and " +
            "mb.accountNumber = ?1 and r.customerDebited = false and r.debitTrialCount = 0")
    List<ReactHQReferralEntity> getCustomerForDebit(String accountNumber);

    @Query("select count(r) from ReactHQReferralEntity r where r.customerCredited = false and r.customerDebited = true")
    long countCustomerAlreadySupported();

    Optional<ReactHQReferralEntity> findTopByCustomer(MintAccountEntity mintAccountEntity);

    List<ReactHQReferralEntity> getAllByCustomerCreditedAndCustomerDebitedOrderByDateCreatedAsc(boolean credited, boolean debited, Pageable pageable);
}
