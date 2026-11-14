
package com.ebookmarketplace.backend.entity;

import com.ebookmarketplace.backend.exception.EbookPdfRequiredException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "suspended_from_status", length = 20)
    private EbookStatus suspendedFromStatus;

    @Column(name = "suspension_reason", length = 1000)
    private String suspensionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspended_by")
    private User suspendedBy;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restored_by")
    private User restoredBy;

    @Column(name = "restored_at")
    private Instant restoredAt;

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
            User createdBy) {
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

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public Category getCategory() {
        return category;
    }

    public User getAuthor() {
        return author;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public String getCoverImagePath() {
        return coverImagePath;
    }

    public String getPdfStoragePath() {
        return pdfStoragePath;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public String getLanguage() {
        return language;
    }

    public EbookStatus getStatus() {
        return status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public EbookStatus getSuspendedFromStatus() {
        return suspendedFromStatus;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public User getSuspendedBy() {
        return suspendedBy;
    }

    public Instant getSuspendedAt() {
        return suspendedAt;
    }

    public User getRestoredBy() {
        return restoredBy;
    }

    public Instant getRestoredAt() {
        return restoredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(
            String title,
            String slug,
            String description,
            BigDecimal price,
            Category category,
            Integer pageCount,
            String language) {
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

        if (this.status == EbookStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Restore the suspended ebook before publishing it");
        }

        if (this.pdfStoragePath == null ||
                this.pdfStoragePath.isBlank()) {
            throw new EbookPdfRequiredException(
                    "Upload a PDF before publishing this ebook");
        }

        this.status = EbookStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void unpublish() {

        if (this.status == EbookStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Restore the suspended ebook before unpublishing it");
        }

        this.status = EbookStatus.UNPUBLISHED;
    }

    public void suspend(String reason, User admin) {

        if (this.status != EbookStatus.PUBLISHED
                && this.status != EbookStatus.UNPUBLISHED) {
            throw new IllegalStateException(
                    "Only published or unpublished ebooks can be suspended");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "A suspension reason is required");
        }

        if (admin == null) {
            throw new IllegalArgumentException(
                    "Administrator is required to suspend an ebook");
        }

        this.suspendedFromStatus = this.status;
        this.suspensionReason = reason.trim();
        this.suspendedBy = admin;
        this.suspendedAt = Instant.now();

        this.restoredBy = null;
        this.restoredAt = null;

        this.status = EbookStatus.SUSPENDED;
    }

    public void restore(User admin) {

        if (this.status != EbookStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Only suspended ebooks can be restored");
        }

        if (admin == null) {
            throw new IllegalArgumentException(
                    "Administrator is required to restore an ebook");
        }

        if (this.suspendedFromStatus == null) {
            throw new IllegalStateException(
                    "Previous ebook status is unavailable");
        }

        this.status = this.suspendedFromStatus;
        this.restoredBy = admin;
        this.restoredAt = Instant.now();
    }
}
