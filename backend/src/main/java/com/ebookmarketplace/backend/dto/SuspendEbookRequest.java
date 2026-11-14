
package com.ebookmarketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendEbookRequest(

        @NotBlank(message = "Suspension reason is required") @Size(max = 1000, message = "Suspension reason cannot exceed 1000 characters") String reason

) {
}
