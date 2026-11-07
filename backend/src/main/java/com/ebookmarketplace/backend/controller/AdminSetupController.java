package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.AdminSetupRequest;
import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.RegisterResponse;
import com.ebookmarketplace.backend.service.AdminSetupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin-setup")
public class AdminSetupController {

    private final AdminSetupService adminSetupService;

    public AdminSetupController(AdminSetupService adminSetupService) {
        this.adminSetupService = adminSetupService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RegisterResponse>> setupAdmin(
            @Valid @RequestBody AdminSetupRequest request
    ) {

        RegisterResponse response =
                adminSetupService.createInitialAdmin(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Administrator account created successfully",
                        response
                ));
    }
}