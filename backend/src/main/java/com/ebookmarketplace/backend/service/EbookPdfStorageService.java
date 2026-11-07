
package com.ebookmarketplace.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class EbookPdfStorageService {

    private final Path storageLocation;
    private final long maxFileSizeBytes;

    public EbookPdfStorageService(
            @Value("${ebook.storage.location}") String storageLocation,
            @Value("${ebook.storage.max-file-size-bytes:52428800}") long maxFileSizeBytes) {
        this.storageLocation = Paths.get(storageLocation)
                .toAbsolutePath()
                .normalize();

        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public String store(Long ebookId, MultipartFile file) {

        validateFile(file);

        try {
            byte[] fileBytes = file.getBytes();

            validatePdfContent(fileBytes);

            String generatedFilename = UUID.randomUUID() + ".pdf";

            Path ebookDirectory = storageLocation.resolve(
                    ebookId.toString()).normalize();

            if (!ebookDirectory.startsWith(storageLocation)) {
                throw new IllegalArgumentException(
                        "Invalid ebook storage location");
            }

            Files.createDirectories(ebookDirectory);

            Path targetPath = ebookDirectory
                    .resolve(generatedFilename)
                    .normalize();

            if (!targetPath.startsWith(storageLocation)) {
                throw new IllegalArgumentException(
                        "Invalid PDF storage path");
            }

            Files.write(
                    targetPath,
                    fileBytes,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);

            return ebookId + "/" + generatedFilename;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to store the PDF file",
                    exception);
        }
    }

    /**
     * Resolves a relative storage key to its physical file path.
     * Prevents access outside the configured storage directory.
     */
    public Path resolve(String storageKey) {

        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Storage key cannot be empty");
        }

        Path targetPath = storageLocation
                .resolve(storageKey)
                .normalize();

        if (!targetPath.startsWith(storageLocation)) {
            throw new IllegalArgumentException(
                    "Invalid storage key");
        }

        if (!Files.isRegularFile(targetPath)
                || !Files.isReadable(targetPath)) {
            throw new IllegalStateException(
                    "Stored PDF does not exist or is not readable");
        }

        return targetPath;
    }

    /**
     * Converts a physical file path into a relative storage key.
     */
    public String toStorageKey(Path filePath) {

        if (filePath == null) {
            throw new IllegalArgumentException(
                    "File path cannot be null");
        }

        Path absolutePath = filePath
                .toAbsolutePath()
                .normalize();

        if (!absolutePath.startsWith(storageLocation)) {
            throw new IllegalArgumentException(
                    "File is outside the configured storage directory");
        }

        return storageLocation
                .relativize(absolutePath)
                .toString()
                .replace('\\', '/');
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a PDF file");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    "PDF file size exceeds the maximum allowed size");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null ||
                !originalFilename.toLowerCase().endsWith(".pdf")) {

            throw new IllegalArgumentException(
                    "Only PDF files are allowed");
        }
    }

    private void validatePdfContent(byte[] fileBytes) {

        if (fileBytes.length < 10) {
            throw new IllegalArgumentException(
                    "Invalid PDF file");
        }

        boolean validHeader = false;

        int headerLimit = Math.min(fileBytes.length, 1024);

        for (int i = 0; i <= headerLimit - 5; i++) {

            if (fileBytes[i] == '%'
                    && fileBytes[i + 1] == 'P'
                    && fileBytes[i + 2] == 'D'
                    && fileBytes[i + 3] == 'F'
                    && fileBytes[i + 4] == '-') {

                validHeader = true;
                break;
            }
        }

        if (!validHeader) {
            throw new IllegalArgumentException(
                    "Uploaded file does not contain a valid PDF signature");
        }

        boolean validEndMarker = false;

        int start = Math.max(0, fileBytes.length - 1024);

        for (int i = start; i <= fileBytes.length - 5; i++) {

            if (fileBytes[i] == '%'
                    && fileBytes[i + 1] == '%'
                    && fileBytes[i + 2] == 'E'
                    && fileBytes[i + 3] == 'O'
                    && fileBytes[i + 4] == 'F') {

                validEndMarker = true;
                break;
            }
        }

        if (!validEndMarker) {
            throw new IllegalArgumentException(
                    "Uploaded file is not a complete PDF document");
        }
    }

    public Path resolvePath(String storageKey) {

        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException(
                    "PDF storage key is required");
        }

        Path targetPath = storageLocation
                .resolve(storageKey)
                .normalize();

        if (!targetPath.startsWith(storageLocation)
                || targetPath.equals(storageLocation)) {
            throw new IllegalArgumentException(
                    "Invalid PDF storage key");
        }

        if (!Files.isRegularFile(targetPath)
                || !Files.isReadable(targetPath)) {
            throw new IllegalStateException(
                    "Stored PDF file does not exist or is not readable");
        }

        return targetPath;
    }

    public void delete(String storageKey) {

        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        try {
            Path targetPath = storageLocation
                    .resolve(storageKey)
                    .normalize();

            if (!targetPath.startsWith(storageLocation)) {
                throw new IllegalArgumentException(
                        "Invalid PDF storage key");
            }

            Files.deleteIfExists(targetPath);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to delete the PDF file",
                    exception);
        }
    }
}
