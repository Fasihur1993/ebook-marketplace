
package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.PurchasedEbookResponse;
import com.ebookmarketplace.backend.service.CustomerLibraryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/library")
public class CustomerLibraryController {

    private final CustomerLibraryService customerLibraryService;

    public CustomerLibraryController(
            CustomerLibraryService customerLibraryService) {
        this.customerLibraryService = customerLibraryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PurchasedEbookResponse>>> getMyLibrary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PurchasedEbookResponse> library = customerLibraryService.getMyLibrary(jwt, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Your library retrieved successfully", library));
    }
}
