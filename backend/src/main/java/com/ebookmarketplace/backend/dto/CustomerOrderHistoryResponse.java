package com.ebookmarketplace.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CustomerOrderHistoryResponse(
        Long orderId,
        BigDecimal totalAmount,
        String currency,
        String status,
        List<OrderItemResponse> items,
        CustomerPaymentAttemptResponse latestPayment,
        Instant createdAt,
        Instant paidAt) {
}