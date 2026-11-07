
package com.ebookmarketplace.backend.exception;

import com.ebookmarketplace.backend.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.ebookmarketplace.backend.exception.EbookPdfRequiredException;

import jakarta.validation.ConstraintViolationException;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .toList();

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure("Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        List<String> errors = exception.getConstraintViolations()
                .stream()
                .map(error ->
                        error.getPropertyPath() + ": " + error.getMessage()
                )
                .toList();

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure("Validation failed", errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(
                        exception.getMessage(),
                        List.of()
                ));
    }

    @ExceptionHandler(EbookPdfAlreadyUploadedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePdfAlreadyUploaded(
            EbookPdfAlreadyUploadedException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(
                        exception.getMessage(),
                        List.of()
                ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.failure(
                        "Uploaded file exceeds the maximum allowed size",
                        List.of()
                ));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(
            EmailAlreadyExistsException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(
                        exception.getMessage(),
                        List.of()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(
                        "Invalid email or password",
                        List.of()
                ));
    }

    @ExceptionHandler(InvalidAdminSetupKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidAdminSetupKey(
            InvalidAdminSetupKeyException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(
                        exception.getMessage(),
                        List.of()
                ));
    }

    @ExceptionHandler(AdminSetupAlreadyCompletedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdminSetupCompleted(
            AdminSetupAlreadyCompletedException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(
                        exception.getMessage(),
                        List.of()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(
                        "The request conflicts with existing data",
                        List.of()
                ));
    }

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleCategoryAlreadyExists(
            CategoryAlreadyExistsException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(
                        exception.getMessage(),
                        List.of()
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(
                        exception.getMessage(),
                        List.of()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception exception
    ) {
        log.error("Unexpected application error", exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        "An unexpected error occurred",
                        List.of()
                ));
    }

    @ExceptionHandler(EbookPdfRequiredException.class)
    public ResponseEntity<ApiResponse<Void>> handlePdfRequired(
            EbookPdfRequiredException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(
                        exception.getMessage(),
                        List.of()
                ));
    }
}
