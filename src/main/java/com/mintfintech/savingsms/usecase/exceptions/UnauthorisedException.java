package com.mintfintech.savingsms.usecase.exceptions;

/**
 * Created by jnwanya on
 * Thu, 13 Feb, 2020
 */
public class UnauthorisedException extends RuntimeException {
    public UnauthorisedException(String message){
        super(message);
    }
}
