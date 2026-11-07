package com.ebookmarketplace.backend.dto;

public record RegisterResponse(
        Long id,
        String fullName,
        String email,
        String role
) {
}