package com.gecili.ecommerce.common.event.payload;

import java.util.List;
import java.util.UUID;

/**
 * Payload of {@code InventoryReleased} (topic {@code inventory.events}, key = orderId).
 *
 * <p>Emitted when previously-held stock is returned to available — either because a
 * reservation's TTL expired or as a compensating action after payment failure/cancellation.
 * Consumed by order-service and notification-service.
 *
 * <p>Releases are idempotent: receiving this (or re-processing it) must never re-add stock
 * for a reservation already in {@code RELEASED}.
 *
 * @param orderId        the order whose stock is released
 * @param reservationIds the reservation ids released
 * @param reason         why the release happened (e.g. {@code TTL_EXPIRED},
 *                       {@code PAYMENT_FAILED}, {@code ORDER_CANCELLED})
 */
public record InventoryReleasedEvent(UUID orderId, List<UUID> reservationIds, String reason) {}
