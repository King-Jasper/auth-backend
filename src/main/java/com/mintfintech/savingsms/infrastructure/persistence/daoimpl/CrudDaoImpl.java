package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.CrudDao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
public class CrudDaoImpl<T, ID> implements CrudDao<T, ID> {

    protected JpaRepository<T, ID> repository;
    public CrudDaoImpl(JpaRepository<T, ID> repository) {
        this.repository = repository;
    }

    @Override
    public Optional<T> findById(ID id) {
        return repository.findById(id);
    }

    @Override
    public T getRecordById(ID id) throws RuntimeException {
        return findById(id).orElseThrow(() -> new RuntimeException("Not found. record with id: "+id));
    }

    @Override
    public T saveRecord(T record) {
        return repository.save(record);
    }
}
