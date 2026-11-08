package com.ebookmarketplace.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long paymentId,
        String paymentMethod,
        String status,
        BigDecimal amount,
        String currency,
        String transactionReference,
        Instant submittedAt) {
}