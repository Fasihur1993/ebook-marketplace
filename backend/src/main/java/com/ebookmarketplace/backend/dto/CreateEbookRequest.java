package com.ebookmarketplace.backend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateEbookRequest(

        @NotBlank(message = "Ebook title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Invalid price")
        BigDecimal price,

        @NotNull(message = "Category is required")
        Long categoryId,

        @Min(value = 1, message = "Page count must be greater than zero")
        Integer pageCount,

        @NotBlank(message = "Language is required")
        @Size(max = 50, message = "Language cannot exceed 50 characters")
        String language
) {
}