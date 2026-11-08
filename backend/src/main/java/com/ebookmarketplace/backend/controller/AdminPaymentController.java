package com.ebookmarketplace.backend.controller;

import com.ebookmarketplace.backend.dto.AdminPaymentActionResponse;
import com.ebookmarketplace.backend.dto.AdminPaymentRejectionRequest;
import com.ebookmarketplace.backend.dto.AdminPaymentResponse;
import com.ebookmarketplace.backend.dto.ApiResponse;
import com.ebookmarketplace.backend.service.PaymentService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/admin/payments")
public class AdminPaymentController {

        private final PaymentService paymentService;

        public AdminPaymentController(PaymentService paymentService) {
                this.paymentService = paymentService;
        }

        // ---------------------------------------------------------
        // List submitted payments
        // ---------------------------------------------------------

        @GetMapping("/submitted")
        public ApiResponse<Page<AdminPaymentResponse>> getSubmittedPayments(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Page<AdminPaymentResponse> payments = paymentService.getSubmittedPayments(page, size);

                return ApiResponse.success(
                                "Submitted payments retrieved successfully",
                                payments);
        }

        // ---------------------------------------------------------
        // View payment screenshot
        // ---------------------------------------------------------

        @GetMapping("/{paymentId}/screenshot")
        public ResponseEntity<Resource> getPaymentScreenshot(
                        @PathVariable Long paymentId) throws IOException {

                Path path = paymentService.getPaymentScreenshot(paymentId);

                Resource resource = new UrlResource(path.toUri());

                MediaType contentType = MediaTypeFactory
                                .getMediaType(resource)
                                .orElse(MediaType.APPLICATION_OCTET_STREAM);

                return ResponseEntity.ok()
                                .contentType(contentType)
                                .cacheControl(CacheControl.noStore())
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                ContentDisposition.inline()
                                                                .filename("payment-screenshot")
                                                                .build()
                                                                .toString())
                                .body(resource);
        }

        // ---------------------------------------------------------
        // Verify payment
        // ---------------------------------------------------------

        @PostMapping("/{paymentId}/verify")
        public ApiResponse<AdminPaymentActionResponse> verifyPayment(
                        @AuthenticationPrincipal Jwt jwt,
                        @PathVariable Long paymentId) {

                AdminPaymentActionResponse response = paymentService.verifyPayment(jwt, paymentId);

                return ApiResponse.success(
                                "Payment verified successfully",
                                response);
        }

        // ---------------------------------------------------------
        // Reject payment
        // ---------------------------------------------------------

        @PostMapping("/{paymentId}/reject")
        public ApiResponse<AdminPaymentActionResponse> rejectPayment(
                        @AuthenticationPrincipal Jwt jwt,
                        @PathVariable Long paymentId,
                        @RequestBody AdminPaymentRejectionRequest request) {

                AdminPaymentActionResponse response = paymentService.rejectPayment(
                                jwt,
                                paymentId,
                                request.reason());

                return ApiResponse.success(
                                "Payment rejected successfully",
                                response);
        }
}