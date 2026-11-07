
-- Ebook categories
CREATE TABLE categories (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Ebook catalogue
CREATE TABLE ebooks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    description TEXT NOT NULL,

    price NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    category_id BIGINT NOT NULL,
    author_id BIGINT,
    created_by BIGINT NOT NULL,

    cover_image_path VARCHAR(500),
    pdf_storage_path VARCHAR(500),

    page_count INTEGER,
    language VARCHAR(50) NOT NULL DEFAULT 'English',

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ebooks_category
        FOREIGN KEY (category_id) REFERENCES categories(id),

    CONSTRAINT fk_ebooks_author
        FOREIGN KEY (author_id) REFERENCES users(id),

    CONSTRAINT fk_ebooks_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),

    CONSTRAINT chk_ebooks_price
        CHECK (price >= 0),

    CONSTRAINT chk_ebooks_page_count
        CHECK (page_count IS NULL OR page_count > 0),

    CONSTRAINT chk_ebooks_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED'))
);

-- Indexes for catalogue queries
CREATE INDEX idx_ebooks_status_created
    ON ebooks(status, created_at DESC);

CREATE INDEX idx_ebooks_category
    ON ebooks(category_id);

CREATE INDEX idx_ebooks_author
    ON ebooks(author_id);
