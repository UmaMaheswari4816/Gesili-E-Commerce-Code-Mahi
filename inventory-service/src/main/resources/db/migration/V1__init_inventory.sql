-- Inventory service schema (database: gesili_inventory).

CREATE TABLE stock_items (
    id         UUID PRIMARY KEY,
    product_id UUID        NOT NULL,
    sku        VARCHAR(64) NOT NULL UNIQUE,
    available  INTEGER     NOT NULL DEFAULT 0 CHECK (available >= 0),
    reserved   INTEGER     NOT NULL DEFAULT 0 CHECK (reserved  >= 0),
    version    BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE reservations (
    id              UUID PRIMARY KEY,
    order_id        UUID        NOT NULL,
    product_id      UUID        NOT NULL,
    sku             VARCHAR(64) NOT NULL,
    quantity        INTEGER     NOT NULL CHECK (quantity > 0),
    status          VARCHAR(16) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_resv_order         ON reservations (order_id);
CREATE INDEX idx_resv_status_expiry ON reservations (status, expires_at);

-- ShedLock table for single-execution scheduled jobs (reservation expiry).
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
