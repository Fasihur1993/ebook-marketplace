package com.ebookmarketplace.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import com.ebookmarketplace.backend.exception.EbookPdfRequiredException;

@Entity
@Table(name = "ebooks")
public class Ebook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "cover_image_path", length = 500)
    private String coverImagePath;

    @Column(name = "pdf_storage_path", length = 500)
    private String pdfStoragePath;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(nullable = false, length = 50)
    private String language = "English";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EbookStatus status = EbookStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Ebook() {
    }

    public Ebook(
        String title,
        String slug,
        String description,
        BigDecimal price,
        Category category,
        User createdBy
    ) {
        this.title = title;
        this.slug = slug;
        this.description = description;
        this.price = price;
        this.category = category;
        this.createdBy = createdBy;
        this.status = EbookStatus.DRAFT;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public Category getCategory() { return category; }
    public User getAuthor() { return author; }
    public User getCreatedBy() { return createdBy; }
    public String getCoverImagePath() { return coverImagePath; }
    public String getPdfStoragePath() { return pdfStoragePath; }
    public Integer getPageCount() { return pageCount; }
    public String getLanguage() { return language; }
    public EbookStatus getStatus() { return status; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateDetails(
        String title,
        String slug,
        String description,
        BigDecimal price,
        Category category,
        Integer pageCount,
        String language
    ) {
        this.title = title;
        this.slug = slug;
        this.description = description;
        this.price = price;
        this.category = category;
        this.pageCount = pageCount;
        this.language = language;
    }

    public void updatePdfStoragePath(String pdfStoragePath) {
        this.pdfStoragePath = pdfStoragePath;
    }

    public void updatePdfMetadata(
            Integer pageCount,
            String coverImagePath) {
        if (pageCount == null || pageCount < 1) {
            throw new IllegalArgumentException(
                    "PDF must contain at least one page");
        }

        if (coverImagePath == null || coverImagePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Cover image path is required");
        }

        this.pageCount = pageCount;
        this.coverImagePath = coverImagePath;
    }

    public void publish() {
        if (this.pdfStoragePath == null ||
                this.pdfStoragePath.isBlank()) {

            throw new EbookPdfRequiredException(
                    "Upload a PDF before publishing this ebook"
            );
        }

        this.status = EbookStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void unpublish() {
        this.status = EbookStatus.UNPUBLISHED;
    }
}