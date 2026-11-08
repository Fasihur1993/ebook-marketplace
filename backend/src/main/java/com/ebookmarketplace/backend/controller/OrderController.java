package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.CreateOrderRequest;
import com.ebookmarketplace.backend.dto.OrderResponse;
import com.ebookmarketplace.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(jwt, request);

        return ApiResponse.success(
                "Order created successfully",
                response);
    }
}