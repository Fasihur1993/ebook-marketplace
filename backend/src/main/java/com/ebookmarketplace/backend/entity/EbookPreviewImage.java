package com.ebookmarketplace.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "ebook_preview_images",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_ebook_preview_images_page",
            columnNames = {"ebook_id", "page_number"}
        )
    }
)
public class EbookPreviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ebook_id", nullable = false)
    private Ebook ebook;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "image_storage_path", nullable = false, length = 500)
    private String imageStoragePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EbookPreviewImage() {
    }

    public EbookPreviewImage(
        Ebook ebook,
        Integer pageNumber,
        String imageStoragePath
    ) {
        this.ebook = ebook;
        this.pageNumber = pageNumber;
        this.imageStoragePath = imageStoragePath;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Ebook getEbook() {
        return ebook;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public String getImageStoragePath() {
        return imageStoragePath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
