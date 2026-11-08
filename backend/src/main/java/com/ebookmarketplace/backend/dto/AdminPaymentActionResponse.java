package com.ebookmarketplace.backend.dto;

import java.time.Instant;

public record AdminPaymentActionResponse(
        Long paymentId,
        String paymentStatus,
        Long orderId,
        String orderStatus,
        Instant verifiedAt,
        String rejectionReason) {
}