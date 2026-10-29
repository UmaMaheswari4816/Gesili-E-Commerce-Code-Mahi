-- Payment service schema (database: gesili_payment).

CREATE TABLE payments (
    id           UUID PRIMARY KEY,
    order_id     UUID         NOT NULL UNIQUE,
    provider     VARCHAR(32)  NOT NULL,
    provider_ref VARCHAR(128),
    amount       NUMERIC(12,2) NOT NULL,
    currency     CHAR(3)      NOT NULL,
    status       VARCHAR(16)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_payment_provider_ref ON payments (provider_ref);

-- Webhook idempotency: one row per provider event id; replays are skipped.
CREATE TABLE webhook_events (
    provider_event_id VARCHAR(128) PRIMARY KEY,
    provider          VARCHAR(32)  NOT NULL,
    received_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ShedLock table for the reconciliation job.
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
