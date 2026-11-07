package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.CreateEbookRequest;
import com.ebookmarketplace.backend.dto.EbookListResponse;
import com.ebookmarketplace.backend.dto.EbookResponse;
import com.ebookmarketplace.backend.service.EbookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/v1/admin/ebooks")
public class AdminEbookController {

        private final EbookService ebookService;

        public AdminEbookController(EbookService ebookService) {
                this.ebookService = ebookService;
        }

        @PostMapping
        public ResponseEntity<ApiResponse<EbookResponse>> createEbook(
                        @Valid @RequestBody CreateEbookRequest request,
                        @AuthenticationPrincipal Jwt jwt) {
                EbookResponse response = ebookService.createEbook(
                                request,
                                jwt.getSubject());

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(
                                                "Ebook created successfully",
                                                response));
        }

        @GetMapping
        public ResponseEntity<ApiResponse<EbookListResponse>> getAllEbooks(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                EbookListResponse response = ebookService.getAllEbooks(
                                page,
                                size);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Ebooks retrieved successfully",
                                                response));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<EbookResponse>> getEbookById(
                        @PathVariable Long id) {
                EbookResponse response = ebookService.getEbookById(id);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Ebook retrieved successfully",
                                                response));
        }

        @PostMapping(value = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<EbookResponse>> uploadPdf(
                        @PathVariable Long id,
                        @RequestParam("file") MultipartFile file) {
                EbookResponse response = ebookService.uploadPdf(id, file);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "PDF uploaded successfully",
                                                response));
        }

        @PostMapping("/{id}/publish")
        public ResponseEntity<ApiResponse<EbookResponse>> publishEbook(
                        @PathVariable Long id) {
                EbookResponse response = ebookService.publishEbook(id);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Ebook published successfully",
                                                response));
        }

        @PostMapping("/{id}/unpublish")
        public ResponseEntity<ApiResponse<EbookResponse>> unpublishEbook(
                        @PathVariable Long id) {
                EbookResponse response = ebookService.unpublishEbook(id);

                return ResponseEntity.ok(
                                ApiResponse.success("Ebook unpublished successfully", response));
        }
}