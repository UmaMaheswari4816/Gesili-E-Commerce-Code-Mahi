-- Order service schema (database: gesili_order).

CREATE TABLE orders (
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL,
    status           VARCHAR(24)  NOT NULL,
    total_amount     NUMERIC(12,2) NOT NULL,
    currency         CHAR(3)      NOT NULL,
    payment_deadline TIMESTAMPTZ,
    cancel_reason    VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE order_items (
    id         UUID PRIMARY KEY,
    order_id   UUID         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id UUID         NOT NULL,
    sku        VARCHAR(64)  NOT NULL,
    quantity   INTEGER      NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_orders_user            ON orders (user_id);
CREATE INDEX idx_orders_status_deadline ON orders (status, payment_deadline);
CREATE INDEX idx_order_items_order      ON order_items (order_id);

-- Transactional outbox. Relay: WHERE published_at IS NULL ORDER BY id (no high-water mark).
CREATE TABLE outbox (
    id           BIGSERIAL PRIMARY KEY,
    topic        VARCHAR(128) NOT NULL,
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(64)  NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox (published_at, id);

-- Consumer idempotency: dedup consumed events by eventId.
CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ShedLock table for the outbox relay + payment-timeout sweep.
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
