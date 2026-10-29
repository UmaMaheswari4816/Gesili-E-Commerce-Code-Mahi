package com.gecili.ecommerce.common.enums;

/**
 * Lifecycle of a stock reservation, owned by the inventory-service.
 *
 * <p>{@link #HELD} reservations carry a TTL (default 20 minutes, intentionally longer than
 * the order payment deadline of 15 minutes so a reservation never expires while a payment
 * is legitimately in flight). The expiry job releases {@code HELD} reservations past their
 * {@code expires_at}.
 *
 * <p>Releases are idempotent and guarded by status: only {@link #HELD} &rarr;
 * {@link #RELEASED} does work; a release of an already-{@link #RELEASED} or
 * {@link #COMMITTED} reservation is a no-op and must never re-add stock.
 */
public enum ReservationStatus {

    /** Stock is held for an order, subject to TTL expiry. */
    HELD,

    /** Payment captured; stock is permanently decremented. */
    COMMITTED,

    /** Stock returned to available (expiry, payment failure, or cancellation). */
    RELEASED
}
