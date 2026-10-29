package com.gesili.ecommerce.common.event.payload;

import java.util.UUID;

/**
 * Payload of {@code OrderCancelled} (topic {@code order.events}, key = orderId).
 *
 * <p>Emitted by order-service on compensation (out of stock, payment failed, payment
 * timeout). Consumed by notification-service and inventory-service.
 *
 * @param orderId the cancelled order
 * @param reason  why it was cancelled (e.g. {@code OUT_OF_STOCK}, {@code PAYMENT_FAILED},
 *                {@code PAYMENT_TIMEOUT})
 */
public record OrderCancelledEvent(UUID orderId, String reason) {}
