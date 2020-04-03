package com.mintfintech.savingsms.domain.services;

/**
 * Created by jnwanya on
 * Thu, 06 Feb, 2020
 */
public interface SequenceGenerator {
    Long nextSequenceId();
    String generateCode(int size);
}
