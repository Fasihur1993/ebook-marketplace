package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.PurchasedEbookResponse;
import com.ebookmarketplace.backend.entity.Purchase;
import com.ebookmarketplace.backend.repository.PurchaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;

@Service
public class CustomerLibraryService {

    private final PurchaseRepository purchaseRepository;
    private final CurrentUserService currentUserService;

    public CustomerLibraryService(
            PurchaseRepository purchaseRepository,
            CurrentUserService currentUserService) {
        this.purchaseRepository = purchaseRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Page<PurchasedEbookResponse> getMyLibrary(
            Jwt jwt,
            int page,
            int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        var user = currentUserService.getRequiredUser(jwt);

        return purchaseRepository
                .findAllByUserIdOrderByPurchasedAtDesc(
                        user.getId(),
                        PageRequest.of(page, size))
                .map(this::toResponse);
    }

    private PurchasedEbookResponse toResponse(Purchase purchase) {

        var ebook = purchase.getEbook();

        return new PurchasedEbookResponse(
                ebook.getId(),
                ebook.getTitle(),
                ebook.getSlug(),
                ebook.getDescription(),
                ebook.getPrice(),
                ebook.getCurrency(),
                ebook.getLanguage(),
                ebook.getPageCount(),
                purchase.getPurchasedAt());
    }
}