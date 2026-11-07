
package com.ebookmarketplace.backend.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfProcessingServiceTest {

    @TempDir
    Path tempDir;

    private PdfProcessingService pdfProcessingService;

    private Path coverDirectory;
    private Path previewDirectory;

    @BeforeEach
    void setUp() {
        coverDirectory = tempDir.resolve("covers");
        previewDirectory = tempDir.resolve("previews");

        pdfProcessingService = new PdfProcessingService(
                coverDirectory.toString(),
                previewDirectory.toString(),
                3,
                120);
    }

    @Test
    void shouldGenerateCoverAndThreePreviewsForFourPagePdf()
            throws IOException {

        Path pdfPath = createPdf("four-pages.pdf", 4);

        PdfProcessingResult result = pdfProcessingService.processPdf(pdfPath, 1L);

        assertEquals(4, result.pageCount());

        assertTrue(Files.exists(result.coverImagePath()));

        assertEquals(3, result.previewImages().size());

        assertEquals(2, result.previewImages().get(0).pageNumber());
        assertEquals(3, result.previewImages().get(1).pageNumber());
        assertEquals(4, result.previewImages().get(2).pageNumber());

        assertTrue(
                Files.exists(result.previewImages().get(0).imagePath()));

        assertTrue(
                Files.exists(result.previewImages().get(1).imagePath()));

        assertTrue(
                Files.exists(result.previewImages().get(2).imagePath()));

        assertValidPng(result.coverImagePath());

        for (PdfProcessingResult.PreviewImage preview : result.previewImages()) {
            assertValidPng(preview.imagePath());
        }
    }

    @Test
    void shouldGenerateOnlyCoverForSinglePagePdf()
            throws IOException {

        Path pdfPath = createPdf("single-page.pdf", 1);

        PdfProcessingResult result = pdfProcessingService.processPdf(pdfPath, 2L);

        assertEquals(1, result.pageCount());

        assertTrue(Files.exists(result.coverImagePath()));

        assertTrue(result.previewImages().isEmpty());

        assertValidPng(result.coverImagePath());
    }

    @Test
    void shouldGenerateAvailablePreviewsForTwoPagePdf()
            throws IOException {

        Path pdfPath = createPdf("two-pages.pdf", 2);

        PdfProcessingResult result = pdfProcessingService.processPdf(pdfPath, 3L);

        assertEquals(2, result.pageCount());

        assertTrue(Files.exists(result.coverImagePath()));

        assertEquals(1, result.previewImages().size());

        assertEquals(2, result.previewImages().get(0).pageNumber());

        assertValidPng(result.previewImages().get(0).imagePath());
    }

    @Test
    void shouldRejectInvalidPdf() throws IOException {

        Path invalidPdf = tempDir.resolve("invalid.pdf");

        Files.writeString(invalidPdf, "This is not a valid PDF");

        assertThrows(
                IOException.class,
                () -> pdfProcessingService.processPdf(invalidPdf, 4L));
    }

    @Test
    void shouldRejectMissingPdf() {

        Path missingPdf = tempDir.resolve("missing.pdf");

        assertThrows(
                IOException.class,
                () -> pdfProcessingService.processPdf(missingPdf, 5L));
    }

    @Test
    void shouldRejectInvalidEbookId() throws IOException {

        Path pdfPath = createPdf("valid.pdf", 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> pdfProcessingService.processPdf(pdfPath, null));

        assertThrows(
                IllegalArgumentException.class,
                () -> pdfProcessingService.processPdf(pdfPath, 0L));
    }

    @Test
    void shouldCreateStorageDirectoriesAutomatically()
            throws IOException {

        Path pdfPath = createPdf("storage-test.pdf", 1);

        assertFalse(Files.exists(coverDirectory));
        assertFalse(Files.exists(previewDirectory));

        pdfProcessingService.processPdf(pdfPath, 6L);

        assertTrue(Files.isDirectory(coverDirectory));
        assertTrue(Files.isDirectory(previewDirectory));
    }

    private Path createPdf(String fileName, int pageCount)
            throws IOException {

        Path pdfPath = tempDir.resolve(fileName);

        try (PDDocument document = new PDDocument()) {

            for (int i = 1; i <= pageCount; i++) {

                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {

                    contentStream.beginText();

                    contentStream.setFont(
                            new PDType1Font(
                                    Standard14Fonts.FontName.HELVETICA),
                            18);

                    contentStream.newLineAtOffset(50, 700);

                    contentStream.showText("Test PDF Page " + i);

                    contentStream.endText();
                }
            }

            document.save(pdfPath.toFile());
        }

        return pdfPath;
    }

    private void assertValidPng(Path imagePath) throws IOException {

        assertTrue(Files.exists(imagePath));

        assertTrue(
                Files.size(imagePath) > 0,
                "Generated image should not be empty");

        BufferedImage image = ImageIO.read(imagePath.toFile());

        assertNotNull(image, "Generated file must be a valid image");

        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);

        image.flush();
    }
}
