
package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.entity.EbookStatus;
import com.ebookmarketplace.backend.repository.PurchaseRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

@Service
public class CustomerEbookDownloadService {

    private final PurchaseRepository purchaseRepository;
    private final CurrentUserService currentUserService;
    private final EbookPdfStorageService pdfStorageService;

    public CustomerEbookDownloadService(
            PurchaseRepository purchaseRepository,
            CurrentUserService currentUserService,
            EbookPdfStorageService pdfStorageService) {
        this.purchaseRepository = purchaseRepository;
        this.currentUserService = currentUserService;
        this.pdfStorageService = pdfStorageService;
    }

    @Transactional(readOnly = true)
    public Path getDownloadPath(Long ebookId, Jwt jwt) {

        var user = currentUserService.getRequiredUser(jwt);

        var purchase = purchaseRepository
                .findByUserIdAndEbookId(user.getId(), ebookId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "You have not purchased this ebook"));

        var ebook = purchase.getEbook();

        if (ebook.getStatus() == EbookStatus.SUSPENDED) {
            throw new AccessDeniedException(
                    "This ebook is temporarily unavailable for download");
        }

        String storagePath = ebook.getPdfStoragePath();

        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalStateException(
                    "Ebook PDF is currently unavailable");
        }

        return pdfStorageService.resolvePath(storagePath);
    }
}
