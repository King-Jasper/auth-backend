package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.ResourceFileEntityDao;
import com.mintfintech.savingsms.domain.entities.ResourceFileEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.ResourceFileRepository;

import javax.inject.Named;
import java.util.Optional;

@Named
public class ResourceFileEntityDaoImpl implements ResourceFileEntityDao {

    private ResourceFileRepository repository;

    public ResourceFileEntityDaoImpl(ResourceFileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ResourceFileEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public ResourceFileEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. ResourceFileEntity with Id :"+aLong));
    }

    @Override
    public ResourceFileEntity saveRecord(ResourceFileEntity record) {
        return repository.save(record);
    }
}
