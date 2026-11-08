
package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.dto.PaymentResponse;
import com.ebookmarketplace.backend.service.PaymentRetryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class PaymentRetryController {

    private final PaymentRetryService paymentRetryService;

    public PaymentRetryController(PaymentRetryService paymentRetryService) {
        this.paymentRetryService = paymentRetryService;
    }

    @PostMapping("/{orderId}/payments/retry")
    public ResponseEntity<ApiResponse<PaymentResponse>> retryPayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Jwt jwt) {
        PaymentResponse response = paymentRetryService.retryPayment(orderId, jwt);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "New payment attempt created successfully",
                        response));
    }
}
