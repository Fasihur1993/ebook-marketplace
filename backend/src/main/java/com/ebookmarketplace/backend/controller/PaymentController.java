package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.PaymentResponse;
import com.ebookmarketplace.backend.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/orders")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{orderId}/payments/{paymentId}/upi")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PaymentResponse> submitUpiPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId,
            @PathVariable Long paymentId,
            @RequestParam String transactionReference,
            @RequestParam MultipartFile screenshot) {
        PaymentResponse response = paymentService.submitUpiPayment(
                jwt,
                orderId,
                paymentId,
                transactionReference,
                screenshot);

        return ApiResponse.success(
                "UPI payment submitted successfully",
                response);
    }
}