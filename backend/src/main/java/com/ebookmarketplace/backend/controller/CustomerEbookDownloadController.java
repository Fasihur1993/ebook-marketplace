
package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.service.CustomerEbookDownloadService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/me/library")
public class CustomerEbookDownloadController {

    private final CustomerEbookDownloadService downloadService;

    public CustomerEbookDownloadController(
            CustomerEbookDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @GetMapping("/{ebookId}/download")
    public ResponseEntity<Resource> downloadEbook(
            @PathVariable Long ebookId,
            @AuthenticationPrincipal Jwt jwt) {
        Path pdfPath = downloadService.getDownloadPath(ebookId, jwt);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ebook-" + ebookId + ".pdf\"")
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(new FileSystemResource(pdfPath));
    }
}
