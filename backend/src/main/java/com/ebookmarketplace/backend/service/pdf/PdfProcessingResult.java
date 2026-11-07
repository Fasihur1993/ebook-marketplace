package com.ebookmarketplace.backend.service.pdf;

import java.nio.file.Path;
import java.util.List;

public record PdfProcessingResult(
        int pageCount,
        Path coverImagePath,
        List<PreviewImage> previewImages) {

    public record PreviewImage(
            int pageNumber,
            Path imagePath) {
    }
}