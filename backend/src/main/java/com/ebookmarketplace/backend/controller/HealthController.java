package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.HealthResponse;
import com.ebookmarketplace.backend.service.HealthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<HealthResponse>> checkHealth() {

        HealthResponse response = healthService.checkHealth();

        if ("DOWN".equals(response.status())) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.failure(
                            "Health check failed",
                            java.util.List.of("Database is unavailable")
                    ));
        }

        return ResponseEntity.ok(
                ApiResponse.success("Health check completed", response)
        );
    }
}