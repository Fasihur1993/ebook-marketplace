package com.ebookmarketplace.backend.exception;

public class EbookPdfRequiredException extends RuntimeException {

    public EbookPdfRequiredException(String message) {
        super(message);
    }
}