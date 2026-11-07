package com.ebookmarketplace.backend.exception;

public class EbookPdfAlreadyUploadedException extends RuntimeException {

    public EbookPdfAlreadyUploadedException(String message) {
        super(message);
    }
}