package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.EbookPreviewImageResponse;
import com.ebookmarketplace.backend.service.EbookService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/admin/ebooks")
public class AdminEbookImageController {

    private final EbookService ebookService;

    public AdminEbookImageController(EbookService ebookService) {
        this.ebookService = ebookService;
    }

    @GetMapping("/{ebookId}/cover")
    public ResponseEntity<Resource> getCover(
            @PathVariable Long ebookId) {

        return imageResponse(
                ebookService.getAdminCoverImagePath(ebookId));
    }

    @GetMapping("/{ebookId}/previews")
    public ApiResponse<List<EbookPreviewImageResponse>> getPreviews(
            @PathVariable Long ebookId) {

        return ApiResponse.success(
                "Preview images retrieved successfully",
                ebookService.getAdminPreviewImages(ebookId));
    }

    @GetMapping("/{ebookId}/previews/{pageNumber}")
    public ResponseEntity<Resource> getPreview(
            @PathVariable Long ebookId,
            @PathVariable Integer pageNumber) {

        return imageResponse(
                ebookService.getAdminPreviewImagePath(
                        ebookId,
                        pageNumber));
    }

    private ResponseEntity<Resource> imageResponse(Path imagePath) {

        Resource resource = new FileSystemResource(imagePath);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(
                        CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header(
                        "X-Content-Type-Options",
                        "nosniff")
                .body(resource);
    }
}