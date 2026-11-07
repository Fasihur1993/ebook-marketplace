package com.ebookmarketplace.backend.service.pdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

@Service
public class PdfProcessingService {

    private final Path coverStorageLocation;
    private final Path previewStorageLocation;
    private final int maxPreviewPages;
    private final float imageDpi;

    public PdfProcessingService(
            @Value("${ebook.storage.cover-location}") String coverLocation,
            @Value("${ebook.storage.preview-location}") String previewLocation,
            @Value("${ebook.preview.max-pages:3}") int maxPreviewPages,
            @Value("${ebook.preview.image-dpi:120}") float imageDpi) {
        this.coverStorageLocation = Path.of(coverLocation);
        this.previewStorageLocation = Path.of(previewLocation);
        this.maxPreviewPages = maxPreviewPages;
        this.imageDpi = imageDpi;
    }

    public PdfProcessingResult processPdf(Path pdfPath, Long ebookId)
            throws IOException {

        if (pdfPath == null || !Files.isRegularFile(pdfPath)
                || !Files.isReadable(pdfPath)) {
            throw new IOException("PDF file does not exist or is not readable");
        }

        if (ebookId == null || ebookId <= 0) {
            throw new IllegalArgumentException("Invalid ebook ID");
        }

        if (maxPreviewPages < 0 || imageDpi <= 0) {
            throw new IllegalStateException("Invalid PDF preview configuration");
        }

        Files.createDirectories(coverStorageLocation);
        Files.createDirectories(previewStorageLocation);

        Path coverPath = coverStorageLocation.resolve(
                "ebook-" + ebookId + "-cover.png");

        List<Path> generatedPreviewPaths = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {

            int pageCount = document.getNumberOfPages();

            if (pageCount < 1) {
                throw new IOException("PDF contains no pages");
            }

            PDFRenderer renderer = new PDFRenderer(document);

            // Generate cover from the first PDF page.
            renderPage(renderer, 0, coverPath);

            // Generate previews from pages 2, 3 and 4 by default.
            int previewCount = Math.min(
                    maxPreviewPages,
                    pageCount - 1);

            List<PdfProcessingResult.PreviewImage> previews = new ArrayList<>();

            for (int i = 0; i < previewCount; i++) {

                int pageIndex = i + 1;
                int pageNumber = pageIndex + 1;

                Path previewPath = previewStorageLocation.resolve(
                        "ebook-" + ebookId + "-preview-page-" +
                                pageNumber + ".png");

                renderPage(renderer, pageIndex, previewPath);

                generatedPreviewPaths.add(previewPath);

                previews.add(
                        new PdfProcessingResult.PreviewImage(
                                pageNumber,
                                previewPath));
            }

            return new PdfProcessingResult(
                    pageCount,
                    coverPath,
                    List.copyOf(previews));

        } catch (IOException | RuntimeException exception) {

            Files.deleteIfExists(coverPath);

            for (Path previewPath : generatedPreviewPaths) {
                Files.deleteIfExists(previewPath);
            }

            throw exception;
        }
    }

    private void renderPage(
            PDFRenderer renderer,
            int pageIndex,
            Path outputPath) throws IOException {

        BufferedImage image = renderer.renderImageWithDPI(
                pageIndex,
                imageDpi,
                ImageType.RGB);

        boolean written = ImageIO.write(
                image,
                "PNG",
                outputPath.toFile());

        if (!written) {
            throw new IOException("Unable to generate PNG image");
        }

        image.flush();
    }
}