package com.mintfintech.savingsms.usecase.exceptions;

/**
 * Created by jnwanya on
 * Sat, 08 Feb, 2020
 */
public class BusinessLogicConflictException extends RuntimeException{
    public BusinessLogicConflictException(String message){
        super(message);
    }
}
