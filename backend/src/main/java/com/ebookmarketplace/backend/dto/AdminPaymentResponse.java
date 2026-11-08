package com.ebookmarketplace.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminPaymentResponse(
                Long paymentId,
                Long orderId,
                String customerName,
                String customerEmail,
                String paymentMethod,
                String status,
                BigDecimal amount,
                String currency,
                String transactionReference,
                Instant submittedAt,
                boolean screenshotAvailable,
                List<AdminPaymentItemResponse> items) {
}