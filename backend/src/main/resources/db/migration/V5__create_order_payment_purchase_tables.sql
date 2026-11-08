
-- =====================================================
-- Orders
-- =====================================================

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMPTZ,

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT chk_orders_total_amount
        CHECK (total_amount > 0),

    CONSTRAINT chk_orders_status
        CHECK (status IN (
            'PENDING_PAYMENT',
            'PAYMENT_SUBMITTED',
            'PAID',
            'CANCELLED',
            'EXPIRED'
        ))
);

CREATE INDEX idx_orders_user_id
    ON orders(user_id);

CREATE INDEX idx_orders_status
    ON orders(status);

CREATE INDEX idx_orders_created_at
    ON orders(created_at DESC);


-- =====================================================
-- Order Items
-- =====================================================

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    ebook_id BIGINT NOT NULL,
    ebook_title VARCHAR(200) NOT NULL,
    author_id BIGINT,
    unit_price NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_items_ebook
        FOREIGN KEY (ebook_id)
        REFERENCES ebooks(id),

    CONSTRAINT fk_order_items_author
        FOREIGN KEY (author_id)
        REFERENCES users(id),

    CONSTRAINT uk_order_items_order_ebook
        UNIQUE (order_id, ebook_id),

    CONSTRAINT chk_order_items_price
        CHECK (unit_price > 0),

    CONSTRAINT chk_order_items_quantity
        CHECK (quantity = 1)
);

CREATE INDEX idx_order_items_order_id
    ON order_items(order_id);

CREATE INDEX idx_order_items_ebook_id
    ON order_items(ebook_id);


-- =====================================================
-- Payments
-- =====================================================

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    -- Manual UPI payment details
    transaction_reference VARCHAR(150),
    screenshot_storage_path VARCHAR(500),

    -- Future Razorpay integration
    provider_payment_id VARCHAR(200),

    -- Admin verification
    verified_by BIGINT,
    verified_at TIMESTAMPTZ,
    rejection_reason VARCHAR(1000),

    submitted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id),

    CONSTRAINT fk_payments_verified_by
        FOREIGN KEY (verified_by)
        REFERENCES users(id),

    CONSTRAINT chk_payments_amount
        CHECK (amount > 0),

    CONSTRAINT chk_payments_method
        CHECK (payment_method IN (
            'UPI_MANUAL',
            'RAZORPAY'
        )),

    CONSTRAINT chk_payments_status
        CHECK (status IN (
            'PENDING',
            'SUBMITTED',
            'VERIFIED',
            'REJECTED',
            'FAILED'
        )),

    CONSTRAINT chk_payments_upi_submission
        CHECK (
            status NOT IN ('SUBMITTED', 'VERIFIED')
            OR (
                payment_method <> 'UPI_MANUAL'
                OR (
                    transaction_reference IS NOT NULL
                    AND screenshot_storage_path IS NOT NULL
                )
            )
        )
);

CREATE INDEX idx_payments_order_id
    ON payments(order_id);

CREATE INDEX idx_payments_status
    ON payments(status);

CREATE INDEX idx_payments_verified_by
    ON payments(verified_by);

-- Prevent duplicate UPI transaction references.
CREATE UNIQUE INDEX uk_payments_transaction_reference
    ON payments(LOWER(transaction_reference))
    WHERE transaction_reference IS NOT NULL;


-- =====================================================
-- Purchases
-- =====================================================

CREATE TABLE purchases (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ebook_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    purchased_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_purchases_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_purchases_ebook
        FOREIGN KEY (ebook_id)
        REFERENCES ebooks(id),

    CONSTRAINT fk_purchases_order_item
        FOREIGN KEY (order_item_id)
        REFERENCES order_items(id),

    -- A customer cannot purchase the same ebook twice.
    CONSTRAINT uk_purchases_user_ebook
        UNIQUE (user_id, ebook_id),

    CONSTRAINT uk_purchases_order_item
        UNIQUE (order_item_id)
);

CREATE INDEX idx_purchases_user_id
    ON purchases(user_id);

CREATE INDEX idx_purchases_ebook_id
    ON purchases(ebook_id);
