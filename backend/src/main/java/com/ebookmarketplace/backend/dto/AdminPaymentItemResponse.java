package com.ebookmarketplace.backend.dto;

import java.math.BigDecimal;

public record AdminPaymentItemResponse(
        Long ebookId,
        String ebookTitle,
        Long authorId,
        BigDecimal unitPrice,
        String currency,
        Integer quantity) {
}