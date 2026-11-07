package com.ebookmarketplace.backend.dto;

import java.util.List;

public record EbookListResponse(
        List<EbookResponse> ebooks,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}