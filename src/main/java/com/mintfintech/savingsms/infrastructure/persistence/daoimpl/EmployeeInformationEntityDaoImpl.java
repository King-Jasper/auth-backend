package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.EmployeeInformationEntityDao;
import com.mintfintech.savingsms.domain.entities.EmployeeInformationEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.EmployeeInformationRepository;

import javax.inject.Named;
import java.util.Optional;

@Named
public class EmployeeInformationEntityDaoImpl implements EmployeeInformationEntityDao {

    private EmployeeInformationRepository repository;

    public EmployeeInformationEntityDaoImpl(EmployeeInformationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<EmployeeInformationEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public EmployeeInformationEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. EmployeeInformationEntity with Id :"+aLong));
    }

    @Override
    public EmployeeInformationEntity saveRecord(EmployeeInformationEntity record) {
        return repository.save(record);
    }
}
