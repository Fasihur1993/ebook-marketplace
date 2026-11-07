
package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.EbookListResponse;
import com.ebookmarketplace.backend.dto.EbookResponse;
import com.ebookmarketplace.backend.service.EbookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ebooks")
public class PublicEbookController {

    private final EbookService ebookService;

    public PublicEbookController(EbookService ebookService) {
        this.ebookService = ebookService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<EbookListResponse>> getPublishedEbooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search) {

        EbookListResponse response = ebookService.getPublishedEbooks(
                page,
                size,
                categoryId,
                search);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Published ebooks retrieved successfully",
                        response));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<EbookResponse>> getPublishedEbookBySlug(
            @PathVariable String slug) {

        EbookResponse response = ebookService.getPublishedEbookBySlug(slug);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Published ebook retrieved successfully",
                        response));
    }
}
