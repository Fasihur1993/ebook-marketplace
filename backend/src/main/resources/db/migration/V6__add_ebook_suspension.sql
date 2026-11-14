
-- Add suspension support to ebooks.

ALTER TABLE ebooks
    DROP CONSTRAINT chk_ebooks_status;

ALTER TABLE ebooks
    ADD CONSTRAINT chk_ebooks_status
    CHECK (status IN (
        'DRAFT',
        'PUBLISHED',
        'UNPUBLISHED',
        'SUSPENDED'
    ));

ALTER TABLE ebooks
    ADD COLUMN suspended_from_status VARCHAR(20),
    ADD COLUMN suspension_reason VARCHAR(1000),
    ADD COLUMN suspended_by BIGINT,
    ADD COLUMN suspended_at TIMESTAMP,
    ADD COLUMN restored_by BIGINT,
    ADD COLUMN restored_at TIMESTAMP;

ALTER TABLE ebooks
    ADD CONSTRAINT fk_ebooks_suspended_by
        FOREIGN KEY (suspended_by) REFERENCES users(id);

ALTER TABLE ebooks
    ADD CONSTRAINT fk_ebooks_restored_by
        FOREIGN KEY (restored_by) REFERENCES users(id);

ALTER TABLE ebooks
    ADD CONSTRAINT chk_ebooks_suspended_from_status
    CHECK (
        suspended_from_status IS NULL
        OR suspended_from_status IN ('PUBLISHED', 'UNPUBLISHED')
    );

ALTER TABLE ebooks
    ADD CONSTRAINT chk_ebooks_suspension_metadata
    CHECK (
        (
            suspended_from_status IS NULL
            AND suspension_reason IS NULL
            AND suspended_by IS NULL
            AND suspended_at IS NULL
        )
        OR
        (
            suspended_from_status IS NOT NULL
            AND suspension_reason IS NOT NULL
            AND suspended_by IS NOT NULL
            AND suspended_at IS NOT NULL
        )
    );

ALTER TABLE ebooks
    ADD CONSTRAINT chk_ebooks_restoration_metadata
    CHECK (
        (restored_by IS NULL AND restored_at IS NULL)
        OR
        (restored_by IS NOT NULL AND restored_at IS NOT NULL)
    );

ALTER TABLE ebooks
    ADD CONSTRAINT chk_ebooks_suspended_not_restored
    CHECK (
        status <> 'SUSPENDED'
        OR (restored_by IS NULL AND restored_at IS NULL)
    );
