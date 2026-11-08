package com.ebookmarketplace.backend.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long ebookId,
        String ebookTitle,
        Long authorId,
        BigDecimal unitPrice,
        String currency,
        Integer quantity) {
}