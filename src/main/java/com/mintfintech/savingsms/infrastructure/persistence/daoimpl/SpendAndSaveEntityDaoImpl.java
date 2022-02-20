package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.CrudDao;
import com.mintfintech.savingsms.domain.dao.SpendAndSaveEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SpendAndSaveEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SpendAndSaveRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.inject.Named;
import java.util.Optional;

@Named
public class SpendAndSaveEntityDaoImpl extends CrudDaoImpl<SpendAndSaveEntity, Long>  implements SpendAndSaveEntityDao {

    private final SpendAndSaveRepository repository;

    public SpendAndSaveEntityDaoImpl(SpendAndSaveRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Optional<SpendAndSaveEntity> findSpendAndSaveByAppUserAndMintAccount(AppUserEntity appUser, MintAccountEntity mintAccount) {
        return repository.findTopByCreatorAndAccountAndRecordStatus(appUser, mintAccount, RecordStatusConstant.ACTIVE);
    }

    @Override
    public Optional<SpendAndSaveEntity> findSpendAndSaveSettingByAccount(MintAccountEntity mintAccount) {
        return repository.findTopByAccountAndRecordStatus(mintAccount, RecordStatusConstant.ACTIVE);
    }
}
