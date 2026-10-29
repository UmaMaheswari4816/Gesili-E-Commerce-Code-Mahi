package com.gecili.ecommerce.common.event.payload;

import java.util.List;
import java.util.UUID;

/**
 * Payload of {@code InventoryReservationFailed} (topic {@code inventory.events}, key =
 * orderId).
 *
 * <p>Emitted when stock cannot be reserved. Consumed by order-service (to cancel the order)
 * and notification-service. This drives a compensating cancellation — no stock was held, so
 * nothing needs releasing.
 *
 * @param orderId     the order that could not be fulfilled
 * @param reason      human-readable reason
 * @param unavailable SKUs that lacked sufficient stock
 */
public record InventoryReservationFailedEvent(
        UUID orderId, String reason, List<String> unavailable) {}
