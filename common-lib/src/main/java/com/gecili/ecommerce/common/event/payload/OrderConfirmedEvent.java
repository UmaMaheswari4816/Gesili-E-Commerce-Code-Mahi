package com.gecili.ecommerce.common.event.payload;

import java.util.UUID;

/**
 * Payload of {@code OrderConfirmed} (topic {@code order.events}, key = orderId).
 *
 * <p>Emitted by order-service when payment is captured. Consumed by notification-service.
 */
public record OrderConfirmedEvent(UUID orderId) {}
