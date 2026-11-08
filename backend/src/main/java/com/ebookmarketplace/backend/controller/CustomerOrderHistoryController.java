package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.CustomerOrderHistoryResponse;
import com.ebookmarketplace.backend.service.CustomerOrderHistoryService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/orders")
public class CustomerOrderHistoryController {

    private final CustomerOrderHistoryService customerOrderHistoryService;

    public CustomerOrderHistoryController(
            CustomerOrderHistoryService customerOrderHistoryService) {
        this.customerOrderHistoryService = customerOrderHistoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerOrderHistoryResponse>>> getOrderHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CustomerOrderHistoryResponse> orders = customerOrderHistoryService.getOrderHistory(jwt, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Order history retrieved successfully", orders));
    }
}