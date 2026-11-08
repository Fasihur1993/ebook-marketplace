
package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.repository.PurchaseRepository;
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

        String storagePath = purchase.getEbook().getPdfStoragePath();

        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalStateException(
                    "Ebook PDF is currently unavailable");
        }

        return pdfStorageService.resolvePath(storagePath);
    }
}
