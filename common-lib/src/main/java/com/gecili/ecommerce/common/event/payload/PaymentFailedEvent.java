package com.gecili.ecommerce.common.event.payload;

import java.util.UUID;

/**
 * Payload of {@code PaymentFailed} (topic {@code payment.events}, key = orderId).
 *
 * <p>Emitted when the provider reports a failed/declined payment. Consumed by order-service
 * (cancel) and inventory-service (release the reservation — compensating transaction).
 *
 * @param orderId   the order whose payment failed
 * @param paymentId the payment aggregate id (may be null if never created)
 * @param reason    provider/decline reason
 */
public record PaymentFailedEvent(UUID orderId, UUID paymentId, String reason) {}
