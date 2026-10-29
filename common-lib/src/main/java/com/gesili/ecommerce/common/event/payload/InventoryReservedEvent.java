package com.gesili.ecommerce.common.event.payload;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload of {@code InventoryReserved} (topic {@code inventory.events}, key = orderId).
 *
 * <p>Emitted by inventory-service after stock is successfully held. Consumed by
 * order-service (to move the order to {@code PAYMENT_PENDING}) and payment-service (to
 * create the provider order/intent).
 *
 * @param orderId      the order the reservation belongs to
 * @param reservations the per-line reservations held
 * @param expiresAt    when the held reservations expire if not committed (TTL, default
 *                     20 minutes — longer than the 15-minute payment deadline)
 */
public record InventoryReservedEvent(
        UUID orderId, List<ReservationLine> reservations, Instant expiresAt) {

    /**
     * A single held reservation line.
     *
     * @param reservationId the reservation id (release idempotency is keyed on this)
     * @param productId     product identifier
     * @param sku           stock-keeping unit
     * @param quantity      quantity held
     */
    public record ReservationLine(UUID reservationId, UUID productId, String sku, int quantity) {}
}
