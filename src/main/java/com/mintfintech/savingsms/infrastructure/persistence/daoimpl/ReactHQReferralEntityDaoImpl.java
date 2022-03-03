package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.ReactHQReferralEntityDao;
import com.mintfintech.savingsms.domain.entities.ReactHQReferralEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.ReactHQReferralRepository;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 03 Mar, 2022
 */
@Named
public class ReactHQReferralEntityDaoImpl extends CrudDaoImpl<ReactHQReferralEntity, Long> implements ReactHQReferralEntityDao {

    private final ReactHQReferralRepository repository;
    public ReactHQReferralEntityDaoImpl(ReactHQReferralRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Optional<ReactHQReferralEntity> findCustomerForDebit(String accountNumber) {
        List<ReactHQReferralEntity> referralList = repository.getCustomerForDebit(accountNumber);
        if(referralList.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(referralList.get(0));
    }
}
