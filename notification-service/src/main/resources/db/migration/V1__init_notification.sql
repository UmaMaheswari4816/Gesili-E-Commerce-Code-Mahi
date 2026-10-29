-- Notification service schema (database: gesili_notification). Audit log + dedup only.

CREATE TABLE notifications (
    id         UUID PRIMARY KEY,
    order_id   UUID,
    channel    VARCHAR(16)  NOT NULL,
    recipient  VARCHAR(255) NOT NULL,
    event_type VARCHAR(64)  NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    delivered  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notif_order ON notifications (order_id);

-- Consumer idempotency: dedup by eventId so redelivery never double-notifies.
CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
