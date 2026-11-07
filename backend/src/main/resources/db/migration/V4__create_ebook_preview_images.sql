
CREATE TABLE ebook_preview_images (
    id BIGSERIAL PRIMARY KEY,
    ebook_id BIGINT NOT NULL,
    page_number INTEGER NOT NULL,
    image_storage_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ebook_preview_images_ebook
        FOREIGN KEY (ebook_id)
        REFERENCES ebooks(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_ebook_preview_images_page
        UNIQUE (ebook_id, page_number),

    CONSTRAINT chk_ebook_preview_images_page_number
        CHECK (page_number > 1)
);

CREATE INDEX idx_ebook_preview_images_ebook_id
    ON ebook_preview_images(ebook_id);
