
package com.ebookmarketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPaymentRequest(
        @NotBlank(message = "Rejection reason is required") @Size(max = 500, message = "Rejection reason cannot exceed 500 characters") String reason) {
}
