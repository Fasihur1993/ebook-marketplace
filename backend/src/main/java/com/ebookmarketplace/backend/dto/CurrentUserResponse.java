package com.ebookmarketplace.backend.dto;

public record CurrentUserResponse(
        Long id,
        String email,
        String role
) {
}