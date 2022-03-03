package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.ReactHQReferralEntity;
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
            "mb.accountNumber = ?1 and r.customerDebited = false")
    List<ReactHQReferralEntity> getCustomerForDebit(String accountNumber);
}
