package com.ebookmarketplace.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class PaymentScreenshotStorageService {

    private final Path storageLocation;
    private final long maxFileSizeBytes;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

    public PaymentScreenshotStorageService(
            @Value("${payment.storage.screenshot-location}") String storageLocation,
            @Value("${payment.storage.max-screenshot-size}") long maxFileSizeBytes) {
        this.storageLocation = Paths.get(storageLocation)
                .toAbsolutePath()
                .normalize();

        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public String store(MultipartFile file) {

        validateFile(file);

        try {
            Files.createDirectories(storageLocation);

            String extension = detectExtension(file);
            String filename = UUID.randomUUID() + extension;

            Path destination = storageLocation.resolve(filename)
                    .normalize();

            if (!destination.startsWith(storageLocation)) {
                throw new IllegalStateException("Invalid screenshot storage path");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(
                        inputStream,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING);
            }

            return filename;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to store payment screenshot",
                    exception);
        }
    }

    public Path resolvePath(String storageKey) {

        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Screenshot storage key is required");
        }

        Path resolvedPath = storageLocation.resolve(storageKey)
                .normalize();

        if (!resolvedPath.startsWith(storageLocation)
                || !Files.isRegularFile(resolvedPath)
                || !Files.isReadable(resolvedPath)) {
            throw new IllegalArgumentException(
                    "Payment screenshot could not be found");
        }

        return resolvedPath;
    }

    public void delete(String storageKey) {

        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        try {
            Path path = storageLocation.resolve(storageKey).normalize();

            if (path.startsWith(storageLocation)) {
                Files.deleteIfExists(path);
            }

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to delete payment screenshot",
                    exception);
        }
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Payment screenshot is required");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    "Payment screenshot must not exceed 5 MB");
        }

        String extension = detectExtension(file);

        if (extension == null) {
            throw new IllegalArgumentException(
                    "Only PNG and JPEG payment screenshots are supported");
        }
    }

    private String detectExtension(MultipartFile file) {

        try (InputStream inputStream = file.getInputStream()) {

            byte[] header = inputStream.readNBytes(8);

            if (header.length >= 8 && matchesPngSignature(header)) {
                return ".png";
            }

            if (header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF) {
                return ".jpg";
            }

            return null;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to inspect payment screenshot",
                    exception);
        }
    }

    private boolean matchesPngSignature(byte[] header) {

        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (header[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }

        return true;
    }
}