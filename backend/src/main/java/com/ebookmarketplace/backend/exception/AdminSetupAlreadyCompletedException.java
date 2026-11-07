package com.ebookmarketplace.backend.exception;

public class AdminSetupAlreadyCompletedException extends RuntimeException {

    public AdminSetupAlreadyCompletedException(String message) {
        super(message);
    }
}