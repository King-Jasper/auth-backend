package com.mintfintech.savingsms.domain.dao;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Wed, 29 Jan, 2020
 */
public interface CrudDao<T, ID> {

    Optional<T> findById(ID id);

    T getRecordById(ID id) throws RuntimeException;

    T saveRecord(T record);
}
