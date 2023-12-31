package com.mintfintech.savingsms.infrastructure.web.controllers.error_handler;

import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.models.ApiResponseJSON;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.validation.ConstraintViolationException;

/**
 * Created by jnwanya on
 * Thu, 06 Feb, 2020
 */
@Slf4j
@ControllerAdvice(basePackages = "com.mintfintech.savingsms.infrastructure.web.controllers")
public class GlobalErrorHandler {

    private final ApplicationProperty applicationProperty;
    public GlobalErrorHandler(ApplicationProperty applicationProperty) {
        this.applicationProperty = applicationProperty;
    }

    @ExceptionHandler(value = {RuntimeException.class, Exception.class})
    public ResponseEntity<ApiResponseJSON<String>> handleException(Exception exception) {
        if(applicationProperty.isLiveEnvironment()) {
            Sentry.captureException(exception);
        }else {
            exception.printStackTrace();
        }
        ApiResponseJSON<String> apiResponse = new ApiResponseJSON<>("Sorry, currently unable to process request at the moment.");
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {MissingServletRequestParameterException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponseJSON<String>> handleMissingServletRequestParameterException(Exception exception) {
        Sentry.captureException(exception);
        ApiResponseJSON<String> apiResponse = new ApiResponseJSON<>(exception.getLocalizedMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponseJSON<String>> handleMissingServletRequestPartException(Exception exception) {
        Sentry.captureException(exception);
        ApiResponseJSON<String> apiResponse = new ApiResponseJSON<>("Request validation failure. Please check your request data.");
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ApiResponseJSON<Object>> handleUnauthorisedOperationException(AccessDeniedException exception) {
        return new ResponseEntity<>(new ApiResponseJSON<>("Sorry, you don't have the required privilege for the request."), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseEntity<ApiResponseJSON<String>> handleMethodArgumentNotValidExceptionException(MethodArgumentNotValidException exception) {
        String errorMessage = "Request validation failure. Please check your request data.";
        BindingResult result = exception.getBindingResult();
        FieldError fieldError = result.getFieldError();
        if(fieldError != null) {
            errorMessage = fieldError.getDefaultMessage();
        }
        log.info("error message: {}", errorMessage);
        ApiResponseJSON<String> apiResponse = new ApiResponseJSON<>(errorMessage);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponseJSON<String>> handleBadRequestException(BadRequestException e) {
        log.info("error message: {}", e.getMessage());
        ApiResponseJSON<String> apiResponse = new ApiResponseJSON<>(e.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessLogicConflictException.class)
    public ResponseEntity<ApiResponseJSON<String>> handleBusinessLogicConflictException(BusinessLogicConflictException e) {
        log.info("error message: {}", e.getMessage());
        ApiResponseJSON<String> apiResponse = new ApiResponseJSON<>(e.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponseJSON<String>> handleNotFoundException(NotFoundException e) {
        log.info("error message: {}", e.getMessage());
        ApiResponseJSON<String> apiResponse = new ApiResponseJSON<>(e.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorisedException.class)
    public ResponseEntity<ApiResponseJSON<String>> handleUnauthorisedExceptionException(UnauthorisedException e) {
        log.info("error message: {}", e.getMessage());
        ApiResponseJSON<String> apiResponse = new ApiResponseJSON<>(e.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }


   /* @ExceptionHandler(RequestForbiddenException.class)
    public ResponseEntity<ApiResponseJSON<String>> handleRequestForbiddenException(RequestForbiddenException e) {
        log.info("error message: {}", e.getMessage());
        ApiResponseJSON<String> apiResponse = new ApiResponseJSON<>(e.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }


    @ExceptionHandler(FailedPreConditionException.class)
    public ResponseEntity<ApiResponseJSON<String>> handleNotFoundException(FailedPreConditionException e) {
        log.info("error message: {}", e.getMessage());
        ApiResponseJSON<String> apiResponse = new ApiResponseJSON<>(e.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.PRECONDITION_FAILED);
    } */
}
