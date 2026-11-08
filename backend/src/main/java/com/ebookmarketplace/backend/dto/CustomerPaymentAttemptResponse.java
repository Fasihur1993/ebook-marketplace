package com.ebookmarketplace.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerPaymentAttemptResponse(
        Long paymentId,
        String paymentMethod,
        String status,
        BigDecimal amount,
        String currency,
        String transactionReference,
        String rejectionReason,
        Instant createdAt,
        Instant submittedAt,
        Instant verifiedAt) {
}