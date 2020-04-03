package com.mintfintech.savingsms.usecase.exceptions;

/**
 * Created by jnwanya on
 * Wed, 05 Feb, 2020
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message){
        super(message);
    }
}
