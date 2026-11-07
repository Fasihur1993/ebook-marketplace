package com.ebookmarketplace.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class EbookImageStorageService {

    private final Path coverStorageLocation;
    private final Path previewStorageLocation;

    public EbookImageStorageService(
            @Value("${ebook.storage.cover-location}") String coverLocation,
            @Value("${ebook.storage.preview-location}") String previewLocation) {
        this.coverStorageLocation = Path.of(coverLocation)
                .toAbsolutePath()
                .normalize();

        this.previewStorageLocation = Path.of(previewLocation)
                .toAbsolutePath()
                .normalize();
    }

    public Path resolveCover(String storageKey) {
        return resolveImage(coverStorageLocation, storageKey);
    }

    public Path resolvePreview(String storageKey) {
        return resolveImage(previewStorageLocation, storageKey);
    }

    private Path resolveImage(Path storageLocation, String storageKey) {

        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Image storage key is required");
        }

        Path targetPath = storageLocation.resolve(storageKey)
                .normalize();

        if (!targetPath.startsWith(storageLocation)
                || targetPath.equals(storageLocation)) {
            throw new IllegalArgumentException(
                    "Invalid image storage key");
        }

        if (!Files.isRegularFile(targetPath)
                || !Files.isReadable(targetPath)) {
            throw new IllegalStateException(
                    "Image file does not exist or is not readable");
        }

        return targetPath;
    }
}