package com.ebookmarketplace.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchasedEbookResponse(
        Long ebookId,
        String title,
        String slug,
        String description,
        BigDecimal price,
        String currency,
        String language,
        Integer pageCount,
        Instant purchasedAt) {
}