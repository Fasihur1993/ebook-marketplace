package com.ebookmarketplace.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record EbookResponse(
        Long id,
        String title,
        String slug,
        String description,
        BigDecimal price,
        String currency,
        Long categoryId,
        String categoryName,
        Long authorId,
        Integer pageCount,
        String language,
        String status,
        boolean pdfUploaded,
        Instant publishedAt,
        Instant createdAt
) {
}