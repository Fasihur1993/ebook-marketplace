
package com.ebookmarketplace.backend.service;

import com.ebookmarketplace.backend.dto.CreateEbookRequest;
import com.ebookmarketplace.backend.dto.EbookListResponse;
import com.ebookmarketplace.backend.dto.EbookResponse;
import com.ebookmarketplace.backend.entity.Category;
import com.ebookmarketplace.backend.entity.Ebook;
import com.ebookmarketplace.backend.entity.EbookPreviewImage;
import com.ebookmarketplace.backend.entity.EbookStatus;
import com.ebookmarketplace.backend.entity.User;
import com.ebookmarketplace.backend.exception.CategoryAlreadyExistsException;
import com.ebookmarketplace.backend.exception.EbookPdfAlreadyUploadedException;
import com.ebookmarketplace.backend.exception.ResourceNotFoundException;
import com.ebookmarketplace.backend.repository.CategoryRepository;
import com.ebookmarketplace.backend.repository.EbookPreviewImageRepository;
import com.ebookmarketplace.backend.repository.EbookRepository;
import com.ebookmarketplace.backend.repository.UserRepository;
import com.ebookmarketplace.backend.service.pdf.PdfProcessingResult;
import com.ebookmarketplace.backend.service.pdf.PdfProcessingService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.ebookmarketplace.backend.dto.EbookPreviewImageResponse;
import java.util.stream.Collectors;

@Service
public class EbookService {

        private final EbookRepository ebookRepository;
        private final CategoryRepository categoryRepository;
        private final UserRepository userRepository;
        private final EbookPdfStorageService pdfStorageService;
        private final PdfProcessingService pdfProcessingService;
        private final EbookPreviewImageRepository ebookPreviewImageRepository;
        private final EbookImageStorageService ebookImageStorageService;

        public EbookService(
                        EbookRepository ebookRepository,
                        CategoryRepository categoryRepository,
                        UserRepository userRepository,
                        EbookPdfStorageService pdfStorageService,
                        PdfProcessingService pdfProcessingService,
                        EbookPreviewImageRepository ebookPreviewImageRepository,
                        EbookImageStorageService ebookImageStorageService) {

                this.ebookRepository = ebookRepository;
                this.categoryRepository = categoryRepository;
                this.userRepository = userRepository;
                this.pdfStorageService = pdfStorageService;
                this.pdfProcessingService = pdfProcessingService;
                this.ebookPreviewImageRepository = ebookPreviewImageRepository;
                this.ebookImageStorageService = ebookImageStorageService;
        }

        @Transactional
        public EbookResponse createEbook(
                        CreateEbookRequest request,
                        String adminEmail) {

                User admin = userRepository.findByEmail(adminEmail)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Administrator not found"));

                Category category = categoryRepository.findById(request.categoryId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Category not found"));

                if (!category.isActive()) {
                        throw new IllegalArgumentException(
                                        "Cannot create an ebook in an inactive category");
                }

                String slug = generateSlug(request.title());

                if (ebookRepository.existsBySlug(slug)) {
                        throw new CategoryAlreadyExistsException(
                                        "An ebook with this title already exists");
                }

                Ebook ebook = new Ebook(
                                request.title().trim(),
                                slug,
                                request.description().trim(),
                                request.price(),
                                category,
                                admin);

                ebook.updateDetails(
                                request.title().trim(),
                                slug,
                                request.description().trim(),
                                request.price(),
                                category,
                                request.pageCount(),
                                request.language().trim());

                Ebook savedEbook = ebookRepository.save(ebook);

                return toResponse(savedEbook);
        }

        @Transactional
        public EbookResponse uploadPdf(Long ebookId, MultipartFile file) {

                Ebook ebook = ebookRepository.findById(ebookId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ebook not found with id: " + ebookId));

                if (ebook.getPdfStoragePath() != null) {
                        throw new EbookPdfAlreadyUploadedException(
                                        "A PDF has already been uploaded for this ebook");
                }

                // Store the original PDF privately.
                String storageKey = pdfStorageService.store(ebookId, file);

                // Remove the original PDF if the database transaction fails.
                registerPdfRollbackCleanup(storageKey);

                // Resolve the relative storage key to its private filesystem path.
                Path pdfPath = pdfStorageService.resolvePath(storageKey);

                // Generate the cover and preview images.
                PdfProcessingResult processingResult;
                try {
                        processingResult = pdfProcessingService.processPdf(
                                        pdfPath,
                                        ebookId);
                } catch (IOException exception) {
                        throw new IllegalStateException(
                                        "Unable to generate ebook cover and preview images",
                                        exception);
                }

                // Remove generated images if the database transaction fails.
                registerImageRollbackCleanup(processingResult);

                // Store relative image keys rather than absolute filesystem paths.
                String coverStorageKey = processingResult.coverImagePath()
                                .getFileName()
                                .toString();

                // Update the ebook's PDF and generated metadata.
                ebook.updatePdfStoragePath(storageKey);

                ebook.updatePdfMetadata(
                                processingResult.pageCount(),
                                coverStorageKey);

                // Create database records for the generated preview images.
                List<EbookPreviewImage> previewImages = new ArrayList<>();

                for (PdfProcessingResult.PreviewImage preview : processingResult.previewImages()) {

                        String previewStorageKey = preview.imagePath()
                                        .getFileName()
                                        .toString();

                        previewImages.add(
                                        new EbookPreviewImage(
                                                        ebook,
                                                        preview.pageNumber(),
                                                        previewStorageKey));
                }

                ebookPreviewImageRepository.saveAll(previewImages);

                Ebook savedEbook = ebookRepository.save(ebook);

                return toResponse(savedEbook);
        }

        private void registerPdfRollbackCleanup(String storageKey) {

                TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronization() {

                                        @Override
                                        public void afterCompletion(int status) {

                                                if (status != STATUS_COMMITTED) {
                                                        try {
                                                                pdfStorageService.delete(storageKey);
                                                        } catch (RuntimeException ignored) {
                                                                // Cleanup failure must not hide the original error.
                                                        }
                                                }
                                        }
                                });
        }

        private void registerImageRollbackCleanup(
                        PdfProcessingResult processingResult) {

                List<Path> generatedPaths = new ArrayList<>();

                generatedPaths.add(processingResult.coverImagePath());

                for (PdfProcessingResult.PreviewImage preview : processingResult.previewImages()) {

                        generatedPaths.add(preview.imagePath());
                }

                List<Path> pathsToDelete = List.copyOf(generatedPaths);

                TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronization() {

                                        @Override
                                        public void afterCompletion(int status) {

                                                if (status != STATUS_COMMITTED) {

                                                        for (Path path : pathsToDelete) {
                                                                try {
                                                                        Files.deleteIfExists(path);
                                                                } catch (IOException ignored) {
                                                                        // Cleanup failure must not hide the original
                                                                        // error.
                                                                }
                                                        }
                                                }
                                        }
                                });
        }

        @Transactional(readOnly = true)
        public EbookListResponse getAllEbooks(int page, int size) {

                validatePagination(page, size);

                Page<Ebook> ebookPage = ebookRepository
                                .findAllByOrderByCreatedAtDesc(
                                                PageRequest.of(page, size));

                return toListResponse(ebookPage);
        }

        @Transactional(readOnly = true)
        public EbookResponse getEbookById(Long id) {

                Ebook ebook = ebookRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ebook not found with id: " + id));

                return toResponse(ebook);
        }

        // Public catalogue: only published ebooks are returned.
        @Transactional(readOnly = true)
        public EbookListResponse getPublishedEbooks(
                        int page,
                        int size,
                        Long categoryId,
                        String search) {

                validatePagination(page, size);

                PageRequest pageable = PageRequest.of(page, size);

                String searchTerm = search == null ? "" : search.trim();

                boolean hasSearch = !searchTerm.isBlank();
                boolean hasCategory = categoryId != null;

                if (hasCategory && categoryId <= 0) {
                        throw new IllegalArgumentException(
                                        "Category ID must be greater than zero");
                }

                Page<Ebook> ebookPage;

                if (hasSearch && hasCategory) {
                        ebookPage = ebookRepository
                                        .findAllByTitleContainingIgnoreCaseAndStatusAndCategoryId(
                                                        searchTerm,
                                                        EbookStatus.PUBLISHED,
                                                        categoryId,
                                                        pageable);
                } else if (hasSearch) {
                        ebookPage = ebookRepository
                                        .findAllByTitleContainingIgnoreCaseAndStatus(
                                                        searchTerm,
                                                        EbookStatus.PUBLISHED,
                                                        pageable);
                } else if (hasCategory) {
                        ebookPage = ebookRepository
                                        .findAllByStatusAndCategoryId(
                                                        EbookStatus.PUBLISHED,
                                                        categoryId,
                                                        pageable);
                } else {
                        ebookPage = ebookRepository.findAllByStatus(
                                        EbookStatus.PUBLISHED,
                                        pageable);
                }

                return toListResponse(ebookPage);
        }

        // Public catalogue: drafts and unpublished ebooks appear as not found.
        @Transactional(readOnly = true)
        public EbookResponse getPublishedEbookBySlug(String slug) {

                Ebook ebook = ebookRepository.findBySlug(slug)
                                .filter(item -> item.getStatus() == EbookStatus.PUBLISHED)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ebook not found with slug: " + slug));

                return toResponse(ebook);
        }

        // Public cover retrieval: only published ebooks are accessible.
        @Transactional(readOnly = true)
        public Path getPublishedCoverImagePath(Long ebookId) {

                Ebook ebook = findEbookOrThrow(ebookId);
                requirePublished(ebook);

                if (ebook.getCoverImagePath() == null
                                || ebook.getCoverImagePath().isBlank()) {
                        throw new ResourceNotFoundException(
                                        "Cover image not found for ebook: " + ebookId);
                }

                return ebookImageStorageService.resolveCover(
                                ebook.getCoverImagePath());
        }

        // Admin cover retrieval: allows drafts and unpublished ebooks.
        @Transactional(readOnly = true)
        public Path getAdminCoverImagePath(Long ebookId) {

                Ebook ebook = findEbookOrThrow(ebookId);

                if (ebook.getCoverImagePath() == null
                                || ebook.getCoverImagePath().isBlank()) {
                        throw new ResourceNotFoundException(
                                        "Cover image not found for ebook: " + ebookId);
                }

                return ebookImageStorageService.resolveCover(
                                ebook.getCoverImagePath());
        }

        // Public preview listing: only published ebooks are accessible.
        @Transactional(readOnly = true)
        public List<EbookPreviewImageResponse> getPublishedPreviewImages(
                        Long ebookId) {

                Ebook ebook = findEbookOrThrow(ebookId);
                requirePublished(ebook);

                return getPreviewImageResponses(ebookId, false);
        }

        // Admin preview listing: allows drafts and unpublished ebooks.
        @Transactional(readOnly = true)
        public List<EbookPreviewImageResponse> getAdminPreviewImages(
                        Long ebookId) {

                findEbookOrThrow(ebookId);

                return getPreviewImageResponses(ebookId, true);
        }

        private List<EbookPreviewImageResponse> getPreviewImageResponses(
                        Long ebookId,
                        boolean admin) {

                String baseUrl = admin
                                ? "/api/v1/admin/ebooks/" + ebookId + "/previews/"
                                : "/api/v1/ebooks/" + ebookId + "/previews/";

                return ebookPreviewImageRepository
                                .findAllByEbookIdOrderByPageNumberAsc(ebookId)
                                .stream()
                                .map(preview -> new EbookPreviewImageResponse(
                                                preview.getPageNumber(),
                                                baseUrl + preview.getPageNumber()))
                                .collect(Collectors.toList());
        }

        // Public preview image retrieval.
        @Transactional(readOnly = true)
        public Path getPublishedPreviewImagePath(
                        Long ebookId,
                        Integer pageNumber) {

                Ebook ebook = findEbookOrThrow(ebookId);
                requirePublished(ebook);

                return resolvePreviewImage(ebookId, pageNumber);
        }

        // Admin preview image retrieval.
        @Transactional(readOnly = true)
        public Path getAdminPreviewImagePath(
                        Long ebookId,
                        Integer pageNumber) {

                findEbookOrThrow(ebookId);

                return resolvePreviewImage(ebookId, pageNumber);
        }

        private Path resolvePreviewImage(
                        Long ebookId,
                        Integer pageNumber) {

                if (pageNumber == null || pageNumber <= 1) {
                        throw new ResourceNotFoundException(
                                        "Preview image not found");
                }

                EbookPreviewImage preview = ebookPreviewImageRepository
                                .findAllByEbookIdOrderByPageNumberAsc(ebookId)
                                .stream()
                                .filter(item -> item.getPageNumber().equals(pageNumber))
                                .findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Preview image not found for page: " + pageNumber));

                return ebookImageStorageService.resolvePreview(
                                preview.getImageStoragePath());
        }

        private Ebook findEbookOrThrow(Long ebookId) {

                return ebookRepository.findById(ebookId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ebook not found with id: " + ebookId));
        }

        private void requirePublished(Ebook ebook) {

                if (ebook.getStatus() != EbookStatus.PUBLISHED) {
                        throw new ResourceNotFoundException(
                                        "Ebook not found with id: " + ebook.getId());
                }
        }

        private void validatePagination(int page, int size) {

                if (page < 0 || size < 1 || size > 100) {
                        throw new IllegalArgumentException(
                                        "Page must be non-negative and size must be between 1 and 100");
                }
        }

        private EbookListResponse toListResponse(Page<Ebook> ebookPage) {

                return new EbookListResponse(
                                ebookPage.getContent()
                                                .stream()
                                                .map(this::toResponse)
                                                .toList(),
                                ebookPage.getNumber(),
                                ebookPage.getSize(),
                                ebookPage.getTotalElements(),
                                ebookPage.getTotalPages());
        }

        private String generateSlug(String value) {

                String normalized = Normalizer.normalize(
                                value,
                                Normalizer.Form.NFD);

                return normalized
                                .replaceAll("\\p{M}", "")
                                .toLowerCase(Locale.ROOT)
                                .replaceAll("[^a-z0-9]+", "-")
                                .replaceAll("^-|-$", "");
        }

        private EbookResponse toResponse(Ebook ebook) {

                return new EbookResponse(
                                ebook.getId(),
                                ebook.getTitle(),
                                ebook.getSlug(),
                                ebook.getDescription(),
                                ebook.getPrice(),
                                ebook.getCurrency(),
                                ebook.getCategory().getId(),
                                ebook.getCategory().getName(),
                                ebook.getAuthor() != null ? ebook.getAuthor().getId() : null,
                                ebook.getPageCount(),
                                ebook.getLanguage(),
                                ebook.getStatus().name(),
                                ebook.getPdfStoragePath() != null,
                                ebook.getPublishedAt(),
                                ebook.getCreatedAt());
        }

        @Transactional
        public EbookResponse publishEbook(Long ebookId) {

                Ebook ebook = ebookRepository.findById(ebookId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ebook not found with id: " + ebookId));

                ebook.publish();

                Ebook savedEbook = ebookRepository.save(ebook);

                return toResponse(savedEbook);
        }

        @Transactional
        public EbookResponse unpublishEbook(Long ebookId) {

                Ebook ebook = ebookRepository.findById(ebookId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ebook not found with id: " + ebookId));

                ebook.unpublish();

                Ebook savedEbook = ebookRepository.save(ebook);

                return toResponse(savedEbook);
        }
}
