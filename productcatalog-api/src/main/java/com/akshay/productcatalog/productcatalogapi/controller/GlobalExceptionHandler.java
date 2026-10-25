package com.akshay.productcatalog.productcatalogapi.controller;

import com.akshay.productcatalog.productcatalogapi.exception.ResourceNotFoundException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Void> handleMethodNotAllowed(){
        return ResponseEntity.status(405)
                .cacheControl(CacheControl.noCache())
                .build();
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Void> handleResourceNotFoundException(
            ResourceNotFoundException exception
    ){
        return  ResponseEntity.notFound().build();
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Void> handleSecurityException(
            SecurityException exception
    ){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .build();
    }















































































}
