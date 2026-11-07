package com.ebookmarketplace.backend.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}