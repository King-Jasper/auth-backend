package com.mintfintech.savingsms.usecase.exceptions;

/**
 * Created by jnwanya on
 * Sat, 08 Feb, 2020
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message){
        super(message);
    }
}
