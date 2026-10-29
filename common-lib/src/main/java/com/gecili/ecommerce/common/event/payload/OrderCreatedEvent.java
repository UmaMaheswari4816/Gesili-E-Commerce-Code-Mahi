package com.gecili.ecommerce.common.event.payload;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Payload of {@code OrderCreated} (topic {@code order.events}, key = orderId).
 *
 * <p>Emitted by order-service after the order and its outbox row are committed in the same
 * transaction. Consumed by inventory-service (to reserve stock) and notification-service.
 *
 * @param orderId     the order aggregate id
 * @param userId      the owning customer
 * @param items       line items to reserve
 * @param totalAmount order total (minor-unit decimal, e.g. rupees)
 * @param currency    ISO-4217 currency code, e.g. {@code INR}
 */
public record OrderCreatedEvent(
        UUID orderId, UUID userId, List<OrderLine> items, BigDecimal totalAmount, String currency) {

    /**
     * A single order line.
     *
     * @param productId product identifier
     * @param sku       stock-keeping unit
     * @param quantity  quantity ordered
     * @param unitPrice price snapshot captured at checkout
     */
    public record OrderLine(UUID productId, String sku, int quantity, BigDecimal unitPrice) {}
}
