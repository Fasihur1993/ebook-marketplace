package com.ebookmarketplace.backend.dto;

public record HealthResponse(
        String status,
        String database
) {
}