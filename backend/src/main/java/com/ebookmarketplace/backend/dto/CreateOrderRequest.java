package com.ebookmarketplace.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(

        @NotEmpty(message = "At least one ebook is required") @Size(max = 20, message = "You can purchase at most 20 ebooks at once") List<@NotNull(message = "Ebook ID cannot be null") Long> ebookIds

) {
}