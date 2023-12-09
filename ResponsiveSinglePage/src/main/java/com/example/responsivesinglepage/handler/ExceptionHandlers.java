package com.example.responsivesinglepage.handler;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ExceptionResponse> response(UserNotFoundException e){
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setDate(LocalDateTime.now().toString());
        exceptionResponse.setMessage(e.getMessage());
        exceptionResponse.setStatusCode(HttpStatus.NOT_FOUND.value());
        return new ResponseEntity<>(exceptionResponse,HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ExceptionResponse> response(InvalidCredentialsException e){
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setDate(LocalDateTime.now().toString());
        exceptionResponse.setMessage(e.getMessage());
        exceptionResponse.setStatusCode(HttpStatus.CONFLICT.value());
        return new ResponseEntity<>(exceptionResponse,HttpStatus.CONFLICT);
    }
}
